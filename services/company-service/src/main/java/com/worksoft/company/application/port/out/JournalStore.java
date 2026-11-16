package com.worksoft.company.application.port.out;

import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.PostingOptions;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.domain.journal.JournalDocument;
import com.worksoft.company.domain.journal.JournalDraft;
import com.worksoft.company.domain.journal.JournalSummary;
import java.time.Instant;
import java.util.UUID;

public interface JournalStore {
  Page<JournalSummary> list(UUID company, SearchPage page);

  JournalDocument get(UUID company, UUID id);

  JournalDocument create(JournalDraft draft, String key, String hash, Instant now);

  JournalDocument update(JournalDraft draft, Instant now);

  JournalDocument post(
      UUID company,
      UUID id,
      long version,
      Instant now,
      com.worksoft.company.application.port.in.Actor actor);

  PostingOptions options(UUID company, String accountQuery, String partyQuery);
}
