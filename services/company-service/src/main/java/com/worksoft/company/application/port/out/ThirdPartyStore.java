package com.worksoft.company.application.port.out;

import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.domain.parties.ThirdParty;
import java.util.UUID;

public interface ThirdPartyStore {
  Page<ThirdParty> list(UUID company, SearchPage page);

  ThirdParty get(UUID company, UUID id);

  ThirdParty create(ThirdParty party);

  ThirdParty update(ThirdParty party);
}
