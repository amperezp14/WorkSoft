package com.worksoft.company.application.port.in;

import com.worksoft.company.domain.journal.JournalDocument;
import com.worksoft.company.domain.journal.JournalHeader;
import com.worksoft.company.domain.journal.JournalLine;
import com.worksoft.company.domain.journal.JournalSummary;
import java.util.List;
import java.util.UUID;

public interface JournalUseCases {
  record Values(JournalHeader header, List<JournalLine> lines, long version) {}

  Page<JournalSummary> list(Actor actor, UUID company, SearchPage page);

  JournalDocument get(Actor actor, UUID company, UUID id);

  JournalDocument create(Actor actor, UUID company, Values values, String key);

  JournalDocument update(Actor actor, UUID company, UUID id, Values values);

  JournalDocument post(Actor actor, UUID company, UUID id, long version);

  PostingOptions options(Actor actor, UUID company, String accountQuery, String partyQuery);
}
