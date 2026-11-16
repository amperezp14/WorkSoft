package com.worksoft.company.application.service;

import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;

import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.in.JournalUseCases;
import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.PostingOptions;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.application.port.out.JournalStore;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.journal.JournalDocument;
import com.worksoft.company.domain.journal.JournalDraft;
import com.worksoft.company.domain.journal.JournalSummary;
import java.time.Clock;
import java.util.UUID;

public final class JournalService implements JournalUseCases {
  private final CompanyAccess access;
  private final JournalStore store;
  private final Clock clock;

  public JournalService(CompanyAccess access, JournalStore store, Clock clock) {
    this.access = access;
    this.store = store;
    this.clock = clock;
  }

  public Page<JournalSummary> list(Actor actor, UUID company, SearchPage page) {
    access.require(actor, company);
    return store.list(company, page);
  }

  public JournalDocument get(Actor actor, UUID company, UUID id) {
    access.require(actor, company);
    return store.get(company, id);
  }

  public JournalDocument create(Actor actor, UUID company, Values v, String key) {
    access.require(actor, company);
    if (key == null || !key.matches("[A-Za-z0-9_-]{16,128}"))
      throw new BusinessException(VALIDATION_ERROR);
    var draft = new JournalDraft(UUID.randomUUID(), company, v.header(), v.lines(), 0);
    return store.create(draft, key, JournalFingerprint.of(draft), clock.instant());
  }

  public JournalDocument update(Actor actor, UUID company, UUID id, Values v) {
    access.require(actor, company);
    return store.update(
        new JournalDraft(id, company, v.header(), v.lines(), v.version()), clock.instant());
  }

  public JournalDocument post(Actor actor, UUID company, UUID id, long version) {
    access.require(actor, company);
    if (version < 0) throw new BusinessException(VALIDATION_ERROR);
    return store.post(company, id, version, clock.instant(), actor);
  }

  public PostingOptions options(Actor actor, UUID company, String a, String p) {
    access.require(actor, company);
    if (a == null || p == null || a.length() > 100 || p.length() > 100)
      throw new BusinessException(VALIDATION_ERROR);
    return store.options(company, a.strip(), p.strip());
  }
}
