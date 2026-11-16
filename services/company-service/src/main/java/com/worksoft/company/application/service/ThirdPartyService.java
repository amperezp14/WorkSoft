package com.worksoft.company.application.service;

import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.application.port.in.ThirdPartyUseCases;
import com.worksoft.company.application.port.out.ThirdPartyStore;
import com.worksoft.company.domain.parties.ThirdParty;
import java.time.Clock;
import java.util.UUID;

public final class ThirdPartyService implements ThirdPartyUseCases {
  private final CompanyAccess access;
  private final ThirdPartyStore store;
  private final Clock clock;

  public ThirdPartyService(CompanyAccess access, ThirdPartyStore store, Clock clock) {
    this.access = access;
    this.store = store;
    this.clock = clock;
  }

  public Page<ThirdParty> list(Actor actor, UUID company, SearchPage page) {
    access.require(actor, company);
    return store.list(company, page);
  }

  public ThirdParty get(Actor actor, UUID company, UUID id) {
    access.require(actor, company);
    return store.get(company, id);
  }

  private ThirdParty party(UUID company, UUID id, Values v, long version) {
    return new ThirdParty(
        id,
        company,
        v.documentType(),
        v.documentNumber(),
        v.verificationDigit(),
        v.name(),
        v.category(),
        v.email(),
        v.phone(),
        v.address(),
        v.active(),
        version,
        clock.instant());
  }

  public ThirdParty create(Actor actor, UUID company, Values v) {
    access.require(actor, company);
    return store.create(party(company, UUID.randomUUID(), v, 0));
  }

  public ThirdParty update(Actor actor, UUID company, UUID id, Values v) {
    access.require(actor, company);
    return store.update(party(company, id, v, v.version()));
  }
}
