package com.worksoft.company.application.port.in;

import com.worksoft.company.domain.parties.ThirdParty;
import java.util.UUID;

public interface ThirdPartyUseCases {
  record Values(
      ThirdParty.DocumentType documentType,
      String documentNumber,
      String verificationDigit,
      String name,
      ThirdParty.Category category,
      String email,
      String phone,
      String address,
      boolean active,
      long version) {}

  Page<ThirdParty> list(Actor actor, UUID company, SearchPage page);

  ThirdParty get(Actor actor, UUID company, UUID id);

  ThirdParty create(Actor actor, UUID company, Values values);

  ThirdParty update(Actor actor, UUID company, UUID id, Values values);
}
