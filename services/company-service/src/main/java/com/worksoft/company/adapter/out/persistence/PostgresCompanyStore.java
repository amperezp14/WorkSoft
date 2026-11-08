package com.worksoft.company.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.support.TransactionTemplate;
import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.domain.model.*;
import com.worksoft.company.domain.exception.BusinessException;
import static com.worksoft.company.domain.exception.BusinessException.Code.*;

public final class PostgresCompanyStore implements CompanyStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private static final String ACCESSIBLE = """
        SELECT c.* FROM companies c JOIN memberships m ON m.company_id=c.id
        WHERE m.issuer=? AND m.subject=? AND m.active=true AND m.role IN ('ADMIN','READER')
        """;
    public PostgresCompanyStore(JdbcTemplate jdbc, TransactionTemplate transactions, Clock clock) {
        this.jdbc = jdbc; this.transactions = transactions; this.clock = clock;
    }
    private Company map(ResultSet rs, int row) throws SQLException {
        return new Company(new CompanyId(rs.getObject("id", UUID.class)), rs.getString("legal_name"),
                new TaxIdentification(rs.getString("nit"), rs.getString("verification_digit")),
                rs.getTimestamp("created_at").toInstant());
    }
    @Override public Optional<Company> findAccessible(Actor actor, CompanyId id) {
        return jdbc.query(ACCESSIBLE + " AND c.id=?", this::map, actor.issuer(), actor.subject(), id.value()).stream().findFirst();
    }
    @Override public List<Company> listAccessible(Actor actor, PagePosition after, int limit) {
        if (after == null) return jdbc.query(ACCESSIBLE + " ORDER BY c.created_at,c.id LIMIT ?", this::map,
                actor.issuer(), actor.subject(), limit);
        return jdbc.query(ACCESSIBLE + " AND (c.created_at,c.id)>(?,?) ORDER BY c.created_at,c.id LIMIT ?", this::map,
                actor.issuer(), actor.subject(), Timestamp.from(after.createdAt()), after.id(), limit);
    }
    @Override public Company provision(Company company, Actor actor, String key, String hash) {
        try {
            return transactions.execute(status -> createInTransaction(company, actor, key, hash));
        } catch (DuplicateKeyException ex) { throw new BusinessException(COMPANY_CONFLICT); }
        catch (TransientDataAccessException ex) { throw new BusinessException(TEMPORARILY_UNAVAILABLE); }
    }
    private Company createInTransaction(Company c, Actor actor, String key, String hash) {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        // El bloqueo es por actor y clave, también entre instancias de la aplicación.
        String lock = actor.issuer().length()+":"+actor.issuer()+actor.subject().length()+":"+actor.subject()+":"+key;
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", rs -> { }, lock);
        var prior = jdbc.query("""
            SELECT request_hash,company_id FROM idempotency
            WHERE issuer=? AND subject=? AND operation='CREATE_COMPANY' AND key=? AND expires_at>?
            """, (rs, n) -> new Previous(rs.getString(1), rs.getObject(2, UUID.class)),
                actor.issuer(), actor.subject(), key, Timestamp.from(clock.instant()));
        if (!prior.isEmpty()) {
            var saved = prior.getFirst();
            // Se reautoriza antes de comparar el cuerpo o reproducir cualquier dato.
            var accessible = findAccessible(actor, new CompanyId(saved.companyId()))
                    .orElseThrow(() -> new BusinessException(FORBIDDEN));
            if (!saved.hash().equals(hash)) throw new BusinessException(IDEMPOTENCY_CONFLICT);
            return accessible;
        }
        jdbc.update("DELETE FROM idempotency WHERE issuer=? AND subject=? AND operation='CREATE_COMPANY' AND key=?",
                actor.issuer(), actor.subject(), key);
        jdbc.update("INSERT INTO companies(id,legal_name,nit,verification_digit,created_at) VALUES (?,?,?,?,?)",
                c.id().value(), c.legalName(), c.taxIdentification().nit(), c.taxIdentification().verificationDigit(), Timestamp.from(c.createdAt()));
        jdbc.update("INSERT INTO memberships(company_id,issuer,subject,role,active) VALUES (?,?,?,'ADMIN',true)",
                c.id().value(), actor.issuer(), actor.subject());
        jdbc.update("""
            INSERT INTO idempotency(issuer,subject,operation,key,request_hash,company_id,expires_at)
            VALUES (?,?,'CREATE_COMPANY',?,?,?,?)
            """, actor.issuer(), actor.subject(), key, hash, c.id().value(), Timestamp.from(clock.instant().plus(Duration.ofHours(24))));
        jdbc.update("INSERT INTO audit(id,company_id,issuer,subject,operation,occurred_at) VALUES (?,?,?,?,'CREATE_COMPANY',?)",
                UUID.randomUUID(), c.id().value(), actor.issuer(), actor.subject(), Timestamp.from(c.createdAt()));
        UUID eventId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO outbox(event_id,company_id,event_type,payload,occurred_at)
            VALUES (?,?,'com.worksoft.company.created.v1',jsonb_build_object(
              'specversion','1.0','id',CAST(? AS text),'source','urn:worksoft:company-service',
              'type','com.worksoft.company.created.v1','subject',CAST(? AS text),
              'time',CAST(? AS text),'datacontenttype','application/json',
              'data',jsonb_build_object('companyId',CAST(? AS text),'status','ACTIVE')),?)
            """, eventId, c.id().value(), eventId.toString(), "companies/"+c.id().value(),
                c.createdAt().toString(), c.id().value().toString(), Timestamp.from(c.createdAt()));
        return c;
    }
    private record Previous(String hash, UUID companyId) { }
}
