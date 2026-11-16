package com.worksoft.company.adapter.out.persistence;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;

import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.application.port.out.ThirdPartyStore;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.parties.ThirdParty;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresThirdPartyStore implements ThirdPartyStore {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final JdbcCompanyLock lock;

  public PostgresThirdPartyStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.lock = new JdbcCompanyLock(jdbc);
  }

  private ThirdParty map(ResultSet rs, int row) throws SQLException {
    return new ThirdParty(
        rs.getObject("id", UUID.class),
        rs.getObject("company_id", UUID.class),
        ThirdParty.DocumentType.valueOf(rs.getString("document_type")),
        rs.getString("document_number"),
        rs.getString("verification_digit"),
        rs.getString("name"),
        ThirdParty.Category.valueOf(rs.getString("category")),
        rs.getString("email"),
        rs.getString("phone"),
        rs.getString("address"),
        rs.getBoolean("active"),
        rs.getLong("version"),
        rs.getTimestamp("updated_at").toInstant());
  }

  public Page<ThirdParty> list(UUID company, SearchPage page) {
    String pattern = SqlPatterns.contains(page.query());
    String where = " WHERE company_id=? AND (name ILIKE ? OR document_number ILIKE ?)";
    long total =
        jdbc.queryForObject(
            "SELECT count(*) FROM third_parties" + where, Long.class, company, pattern, pattern);
    var items =
        jdbc.query(
            "SELECT * FROM third_parties" + where + " ORDER BY name,id LIMIT ? OFFSET ?",
            this::map,
            company,
            pattern,
            pattern,
            page.size(),
            page.page() * page.size());
    return new Page<>(items, total, page.page(), page.size());
  }

  public ThirdParty get(UUID company, UUID id) {
    return jdbc
        .query("SELECT * FROM third_parties WHERE company_id=? AND id=?", this::map, company, id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(PARTY_NOT_FOUND));
  }

  public ThirdParty create(ThirdParty p) {
    try {
      return transactions.execute(
          status -> {
            lock.acquire(p.companyId());
            jdbc.update(
                """
                INSERT INTO third_parties(id,company_id,document_type,document_number,verification_digit,name,category,email,phone,address,active,version,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,0,?)
                """,
                p.id(),
                p.companyId(),
                p.documentType().name(),
                p.documentNumber(),
                p.verificationDigit(),
                p.name(),
                p.category().name(),
                p.email(),
                p.phone(),
                p.address(),
                p.active(),
                Timestamp.from(p.updatedAt()));
            return get(p.companyId(), p.id());
          });
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(PARTY_CONFLICT);
    }
  }

  public ThirdParty update(ThirdParty p) {
    try {
      return transactions.execute(
          status -> {
            lock.acquire(p.companyId());
            var old = get(p.companyId(), p.id());
            if (old.version() != p.version()) throw new BusinessException(VERSION_CONFLICT);
            int changed =
                jdbc.update(
                    """
                    UPDATE third_parties SET document_type=?,document_number=?,verification_digit=?,name=?,category=?,email=?,phone=?,address=?,active=?,version=version+1,updated_at=?
                    WHERE company_id=? AND id=? AND version=?
                    """,
                    p.documentType().name(),
                    p.documentNumber(),
                    p.verificationDigit(),
                    p.name(),
                    p.category().name(),
                    p.email(),
                    p.phone(),
                    p.address(),
                    p.active(),
                    Timestamp.from(p.updatedAt()),
                    p.companyId(),
                    p.id(),
                    p.version());
            if (changed != 1) throw new BusinessException(VERSION_CONFLICT);
            return get(p.companyId(), p.id());
          });
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(PARTY_CONFLICT);
    }
  }
}
