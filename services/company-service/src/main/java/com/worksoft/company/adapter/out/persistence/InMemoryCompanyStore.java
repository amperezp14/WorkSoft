package com.worksoft.company.adapter.out.persistence;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.domain.model.*;
import com.worksoft.company.domain.exception.BusinessException;
import static com.worksoft.company.domain.exception.BusinessException.Code.*;
/** Almacenamiento volátil para el perfil local; no abre conexiones externas. */
public final class InMemoryCompanyStore implements CompanyStore {
    private record Owner(String issuer, String subject) { }
    private record Entry(Company company, Owner owner) { }
    private record Request(Owner owner, String key) { }
    private record Saved(String hash, UUID id, Instant expires) { }
    private final Map<UUID, Entry> companies = new LinkedHashMap<>();
    private final Map<Request, Saved> requests = new HashMap<>();
    private final Clock clock;
    public InMemoryCompanyStore(Clock clock) { this.clock = clock; }
    private Owner owner(Actor actor) { return new Owner(actor.issuer(), actor.subject()); }
    @Override public synchronized Company provision(Company candidate, Actor actor, String key, String hash) {
        var request = new Request(owner(actor), key);
        var saved = requests.get(request);
        if (saved != null && saved.expires().isAfter(clock.instant())) {
            if (!saved.hash().equals(hash)) throw new BusinessException(IDEMPOTENCY_CONFLICT);
            return companies.get(saved.id()).company();
        }
        if (companies.values().stream().anyMatch(e -> e.company().taxIdentification().nit().equals(candidate.taxIdentification().nit())))
            throw new BusinessException(COMPANY_CONFLICT);
        companies.put(candidate.id().value(), new Entry(candidate, owner(actor)));
        requests.put(request, new Saved(hash, candidate.id().value(), clock.instant().plus(Duration.ofHours(24))));
        return candidate;
    }
    @Override public synchronized Optional<Company> findAccessible(Actor actor, CompanyId id) {
        var entry = companies.get(id.value());
        return entry != null && entry.owner().equals(owner(actor)) ? Optional.of(entry.company()) : Optional.empty();
    }
    @Override public synchronized List<Company> listAccessible(Actor actor, PagePosition after, int limit) {
        return companies.values().stream().filter(e -> e.owner().equals(owner(actor))).map(Entry::company)
            .filter(c -> after == null || c.createdAt().isAfter(after.createdAt()) ||
                (c.createdAt().equals(after.createdAt()) && c.id().value().toString().compareTo(after.id().toString()) > 0))
            .sorted(Comparator.comparing(Company::createdAt).thenComparing(c -> c.id().value().toString())).limit(limit).toList();
    }
}
