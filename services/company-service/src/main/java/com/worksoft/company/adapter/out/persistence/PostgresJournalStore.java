package com.worksoft.company.adapter.out.persistence;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.PostingOptions;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.application.port.out.JournalStore;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.journal.JournalDocument;
import com.worksoft.company.domain.journal.JournalDocument.*;
import com.worksoft.company.domain.journal.JournalDraft;
import com.worksoft.company.domain.journal.JournalHeader;
import com.worksoft.company.domain.journal.JournalLine;
import com.worksoft.company.domain.journal.JournalSummary;
import com.worksoft.company.domain.journal.PostingRules;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresJournalStore implements JournalStore {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final JdbcCompanyLock lock;

  public PostgresJournalStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.lock = new JdbcCompanyLock(jdbc);
  }

  private record AccountRef(
      AccountSnapshot snapshot, boolean active, boolean posting, boolean children) {}

  private record PartyRef(PartySnapshot snapshot, boolean active) {}

  private String placeholders(int count) {
    return String.join(",", Collections.nCopies(count, "?"));
  }

  private List<ResolvedLine> resolve(UUID company, List<JournalLine> lines) {
    var accountIds = lines.stream().map(JournalLine::accountId).distinct().toList();
    var partyIds = lines.stream().map(JournalLine::partyId).distinct().toList();
    var accountArgs = new ArrayList<Object>();
    accountArgs.add(company);
    accountArgs.addAll(accountIds);
    var partyArgs = new ArrayList<Object>();
    partyArgs.add(company);
    partyArgs.addAll(partyIds);
    Map<UUID, AccountRef> accounts = new HashMap<>();
    Map<UUID, PartyRef> parties = new HashMap<>();
    jdbc.query(
        """
        SELECT a.*,EXISTS(SELECT 1 FROM chart_accounts child WHERE child.company_id=a.company_id AND child.parent_code=a.code) AS has_children
        FROM chart_accounts a WHERE a.company_id=? AND a.id IN (
        """
            + placeholders(accountIds.size())
            + ")",
        (rs, n) -> {
          var snapshot =
              new AccountSnapshot(
                  rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"));
          accounts.put(
              snapshot.id(),
              new AccountRef(
                  snapshot,
                  rs.getBoolean("active"),
                  rs.getBoolean("allows_posting"),
                  rs.getBoolean("has_children")));
          return snapshot.id();
        },
        accountArgs.toArray());
    jdbc.query(
        "SELECT * FROM third_parties WHERE company_id=? AND id IN ("
            + placeholders(partyIds.size())
            + ")",
        (rs, n) -> {
          var snapshot =
              new PartySnapshot(
                  rs.getObject("id", UUID.class),
                  rs.getString("document_type"),
                  rs.getString("document_number"),
                  rs.getString("verification_digit"),
                  rs.getString("name"));
          parties.put(snapshot.id(), new PartyRef(snapshot, rs.getBoolean("active")));
          return snapshot.id();
        },
        partyArgs.toArray());
    return lines.stream()
        .map(
            line -> {
              var account = accounts.get(line.accountId());
              var party = parties.get(line.partyId());
              if (account == null || party == null)
                throw new BusinessException(POSTING_REFERENCE_INVALID);
              PostingRules.requireEligible(
                  account.active(), account.posting(), account.children(), party.active());
              return new ResolvedLine(line, account.snapshot(), party.snapshot());
            })
        .toList();
  }

  private CompanySnapshot company(UUID company) {
    return jdbc.queryForObject(
        "SELECT legal_name,nit,verification_digit FROM companies WHERE id=?",
        (rs, n) -> new CompanySnapshot(rs.getString(1), rs.getString(2), rs.getString(3)),
        company);
  }

  private void replaceLines(JournalDraft draft, List<ResolvedLine> lines) {
    jdbc.update(
        "DELETE FROM journal_lines WHERE company_id=? AND voucher_id=?",
        draft.companyId(),
        draft.id());
    int position = 0;
    for (var line : lines) {
      var e = line.entry();
      var a = line.account();
      var p = line.party();
      jdbc.update(
          """
          INSERT INTO journal_lines(company_id,voucher_id,line_number,account_id,account_code,account_name,
              party_id,party_document_type,party_document_number,party_dv,party_name,description,debit,credit)
          VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
          """,
          draft.companyId(),
          draft.id(),
          ++position,
          a.id(),
          a.code(),
          a.name(),
          p.id(),
          p.documentType(),
          p.documentNumber(),
          p.verificationDigit(),
          p.name(),
          e.description(),
          e.debit(),
          e.credit());
    }
  }

  public JournalDocument create(JournalDraft draft, String key, String hash, Instant now) {
    return transactions.execute(
        status -> {
          lock.acquire(draft.companyId());
          var previous =
              jdbc.query(
                  "SELECT id,request_hash FROM journal_vouchers WHERE company_id=? AND"
                      + " request_key=?",
                  (rs, n) -> Map.entry(rs.getObject(1, UUID.class), rs.getString(2)),
                  draft.companyId(),
                  key);
          if (!previous.isEmpty()) {
            if (!previous.getFirst().getValue().equals(hash))
              throw new BusinessException(IDEMPOTENCY_CONFLICT);
            return get(draft.companyId(), previous.getFirst().getKey());
          }
          var lines = resolve(draft.companyId(), draft.lines());
          var c = company(draft.companyId());
          var h = draft.header();
          jdbc.update(
              """
              INSERT INTO journal_vouchers(id,company_id,request_key,request_hash,company_name,company_nit,company_dv,entry_date,entry_year,voucher_type,
                  origin,description,support_reference,prepared_by,status,total_debit,total_credit,version,created_at,updated_at)
              VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,'DRAFT',?,?,0,?,?)
              """,
              draft.id(),
              draft.companyId(),
              key,
              hash,
              c.name(),
              c.nit(),
              c.verificationDigit(),
              h.date(),
              h.date().getYear(),
              h.type().name(),
              h.origin(),
              h.description(),
              h.supportReference(),
              h.preparedBy(),
              draft.totalDebit(),
              draft.totalCredit(),
              Timestamp.from(now),
              Timestamp.from(now));
          replaceLines(draft, lines);
          return get(draft.companyId(), draft.id());
        });
  }

  public JournalDocument update(JournalDraft draft, Instant now) {
    return transactions.execute(
        status -> {
          lock.acquire(draft.companyId());
          get(draft.companyId(), draft.id()).requireEditable(draft.version());
          var lines = resolve(draft.companyId(), draft.lines());
          var c = company(draft.companyId());
          var h = draft.header();
          jdbc.update(
              """
              UPDATE journal_vouchers SET company_name=?,company_nit=?,company_dv=?,entry_date=?,entry_year=?,voucher_type=?,origin=?,description=?,support_reference=?,prepared_by=?,
                  total_debit=?,total_credit=?,version=version+1,updated_at=? WHERE company_id=? AND id=?
              """,
              c.name(),
              c.nit(),
              c.verificationDigit(),
              h.date(),
              h.date().getYear(),
              h.type().name(),
              h.origin(),
              h.description(),
              h.supportReference(),
              h.preparedBy(),
              draft.totalDebit(),
              draft.totalCredit(),
              Timestamp.from(now),
              draft.companyId(),
              draft.id());
          replaceLines(draft, lines);
          return get(draft.companyId(), draft.id());
        });
  }

  public JournalDocument post(UUID companyId, UUID id, long version, Instant now, Actor actor) {
    return transactions.execute(
        status -> {
          lock.acquire(companyId);
          var old = get(companyId, id);
          if (old.status() == Status.POSTED) return old;
          old.requireEditable(version);
          var draft = old.draft();
          draft.requireBalanced();
          var lines = resolve(companyId, draft.lines());
          var c = company(companyId);
          replaceLines(draft, lines);
          long number =
              jdbc.queryForObject(
                  """
                  INSERT INTO journal_sequences(company_id,entry_year,last_number) VALUES (?,?,1)
                  ON CONFLICT(company_id,entry_year) DO UPDATE SET last_number=journal_sequences.last_number+1 RETURNING last_number
                  """,
                  Long.class,
                  companyId,
                  draft.header().date().getYear());
          jdbc.update(
              """
              UPDATE journal_vouchers SET status='POSTED',number=?,posted_at=?,updated_at=?,version=version+1,company_name=?,company_nit=?,company_dv=?
              WHERE company_id=? AND id=?
              """,
              number,
              Timestamp.from(now),
              Timestamp.from(now),
              c.name(),
              c.nit(),
              c.verificationDigit(),
              companyId,
              id);
          UUID event = UUID.randomUUID();
          jdbc.update(
              "INSERT INTO audit(id,company_id,issuer,subject,operation,occurred_at) VALUES"
                  + " (?,?,?,?,'POST_JOURNAL',?)",
              UUID.randomUUID(),
              companyId,
              actor.issuer(),
              actor.subject(),
              Timestamp.from(now));
          jdbc.update(
              """
              INSERT INTO outbox(event_id,company_id,event_type,payload,occurred_at)
              VALUES (?,?,'com.worksoft.journal.posted.v1',jsonb_build_object('eventId',CAST(? AS text),'companyId',CAST(? AS text),'voucherId',CAST(? AS text),'number',CAST(? AS bigint),'currency','COP'),?)
              """,
              event,
              companyId,
              event.toString(),
              companyId.toString(),
              id.toString(),
              number,
              Timestamp.from(now));
          return get(companyId, id);
        });
  }

  private ResolvedLine line(ResultSet rs, int n) throws SQLException {
    var entry =
        new JournalLine(
            rs.getObject("account_id", UUID.class),
            rs.getObject("party_id", UUID.class),
            rs.getString("description"),
            rs.getBigDecimal("debit"),
            rs.getBigDecimal("credit"));
    return new ResolvedLine(
        entry,
        new AccountSnapshot(
            entry.accountId(), rs.getString("account_code"), rs.getString("account_name")),
        new PartySnapshot(
            entry.partyId(),
            rs.getString("party_document_type"),
            rs.getString("party_document_number"),
            rs.getString("party_dv"),
            rs.getString("party_name")));
  }

  public JournalDocument get(UUID company, UUID id) {
    return transactions.execute(status -> readDocument(company, id));
  }

  private JournalDocument readDocument(UUID company, UUID id) {
    jdbc.execute("SET LOCAL lock_timeout='5s'");
    var found =
        jdbc.query(
            "SELECT id FROM journal_vouchers WHERE company_id=? AND id=? FOR SHARE",
            (rs, n) -> rs.getObject(1, UUID.class),
            company,
            id);
    if (found.isEmpty()) throw new BusinessException(VOUCHER_NOT_FOUND);
    var lines =
        jdbc.query(
            "SELECT * FROM journal_lines WHERE company_id=? AND voucher_id=? ORDER BY line_number",
            this::line,
            company,
            id);
    var documents =
        jdbc.query(
            "SELECT * FROM journal_vouchers WHERE company_id=? AND id=?",
            (rs, n) -> {
              var header =
                  new JournalHeader(
                      rs.getObject("entry_date", LocalDate.class),
                      JournalHeader.Type.valueOf(rs.getString("voucher_type")),
                      rs.getString("origin"),
                      rs.getString("description"),
                      rs.getString("support_reference"),
                      rs.getString("prepared_by"));
              var draft =
                  new JournalDraft(
                      id,
                      company,
                      header,
                      lines.stream().map(ResolvedLine::entry).toList(),
                      rs.getLong("version"));
              return new JournalDocument(
                  draft,
                  Status.valueOf(rs.getString("status")),
                  rs.getObject("number", Long.class),
                  new CompanySnapshot(
                      rs.getString("company_name"),
                      rs.getString("company_nit"),
                      rs.getString("company_dv")),
                  lines,
                  rs.getTimestamp("created_at").toInstant(),
                  rs.getTimestamp("updated_at").toInstant(),
                  rs.getTimestamp("posted_at") == null
                      ? null
                      : rs.getTimestamp("posted_at").toInstant());
            },
            company,
            id);
    return documents.stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(VOUCHER_NOT_FOUND));
  }

  public Page<JournalSummary> list(UUID company, SearchPage page) {
    String pattern = SqlPatterns.contains(page.query());
    String where =
        " WHERE company_id=? AND (description ILIKE ? OR coalesce(number::text,'') ILIKE ?)";
    long total =
        jdbc.queryForObject(
            "SELECT count(*) FROM journal_vouchers" + where, Long.class, company, pattern, pattern);
    var items =
        jdbc.query(
            "SELECT * FROM journal_vouchers"
                + where
                + " ORDER BY entry_date DESC,created_at DESC,id LIMIT ? OFFSET ?",
            (rs, n) ->
                new JournalSummary(
                    rs.getObject("id", UUID.class),
                    rs.getObject("entry_date", LocalDate.class),
                    JournalHeader.Type.valueOf(rs.getString("voucher_type")),
                    Status.valueOf(rs.getString("status")),
                    rs.getObject("number", Long.class),
                    rs.getString("description"),
                    rs.getBigDecimal("total_debit"),
                    rs.getBigDecimal("total_credit"),
                    rs.getLong("version")),
            company,
            pattern,
            pattern,
            page.size(),
            page.page() * page.size());
    return new Page<>(items, total, page.page(), page.size());
  }

  public PostingOptions options(UUID company, String accountQuery, String partyQuery) {
    String a = SqlPatterns.contains(accountQuery), p = SqlPatterns.contains(partyQuery);
    var accounts =
        jdbc.query(
            """
            SELECT id,code,name FROM chart_accounts a WHERE company_id=? AND active AND allows_posting
            AND NOT EXISTS(SELECT 1 FROM chart_accounts child WHERE child.company_id=a.company_id AND child.parent_code=a.code)
            AND (code ILIKE ? OR name ILIKE ?) ORDER BY code LIMIT 51
            """,
            (rs, n) ->
                new PostingOptions.AccountOption(
                    rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)),
            company,
            a,
            a);
    var parties =
        jdbc.query(
            "SELECT id,document_type,document_number,verification_digit,name FROM third_parties"
                + " WHERE company_id=? AND active AND (name ILIKE ? OR document_number ILIKE ?)"
                + " ORDER BY name,id LIMIT 51",
            (rs, n) ->
                new PostingOptions.PartyOption(
                    rs.getObject(1, UUID.class),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5)),
            company,
            p,
            p);
    return new PostingOptions(
        accounts.subList(0, Math.min(accounts.size(), 50)),
        parties.subList(0, Math.min(parties.size(), 50)),
        accounts.size() > 50,
        parties.size() > 50);
  }
}
