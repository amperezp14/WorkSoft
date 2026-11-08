package com.worksoft.company.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.domain.model.*;
import com.worksoft.company.domain.exception.BusinessException;
import static com.worksoft.company.domain.exception.BusinessException.Code.*;

public final class CompanyService implements CreateCompanyUseCase, QueryCompaniesUseCase {
    private final CompanyStore store;
    private final CursorCodec cursors;
    private final Clock clock;
    public CompanyService(CompanyStore store, CursorCodec cursors, Clock clock) {
        this.store = store; this.cursors = cursors; this.clock = clock;
    }
    @Override public Company create(Actor actor, CreateCompanyCommand command) {
        if (!actor.canProvision()) throw new BusinessException(FORBIDDEN);
        if (command.idempotencyKey() == null || !command.idempotencyKey().matches("[A-Za-z0-9_-]{16,128}"))
            throw new BusinessException(VALIDATION_ERROR);
        var company = new Company(new CompanyId(UUID.randomUUID()), command.legalName(),
                new TaxIdentification(command.nit(), command.verificationDigit()),
                clock.instant().truncatedTo(ChronoUnit.MICROS));
        // Longitudes prefijadas para evitar ambigüedades en el hash de los campos normalizados.
        String input = field(company.legalName()) + field(command.nit()) + field(command.verificationDigit());
        return store.provision(company, actor, command.idempotencyKey(), hash(input));
    }
    private String field(String value) { return value.length() + ":" + value; }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    @Override public Company get(Actor actor, CompanyId id) {
        return store.findAccessible(actor, id).orElseThrow(() -> new BusinessException(COMPANY_NOT_FOUND));
    }
    @Override public CompanyPage list(Actor actor, int limit, String cursor) {
        if (limit < 1 || limit > 100) throw new BusinessException(VALIDATION_ERROR);
        PagePosition after = cursor == null ? null : cursors.decode(cursor, actor, limit);
        var found = store.listAccessible(actor, after, limit + 1);
        boolean more = found.size() > limit;
        var page = found.subList(0, Math.min(limit, found.size()));
        String next = null;
        if (more) {
            var last = page.getLast();
            next = cursors.encode(new PagePosition(last.createdAt(), last.id().value()), actor, limit);
        }
        return new CompanyPage(page, next);
    }
}
