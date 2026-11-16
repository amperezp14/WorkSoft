package com.worksoft.company.adapter.in.web;

import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.application.port.in.ThirdPartyUseCases;
import com.worksoft.company.domain.parties.ThirdParty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/companies/{companyId}/third-parties")
public class ThirdPartyController {
  private final ThirdPartyUseCases service;

  public ThirdPartyController(ThirdPartyUseCases service) {
    this.service = service;
  }

  public record Request(
      @NotNull ThirdParty.DocumentType documentType,
      @NotBlank @Size(max = 30) String documentNumber,
      @Size(max = 1) String verificationDigit,
      @NotBlank @Size(max = 200) String name,
      @NotNull ThirdParty.Category category,
      @Email @Size(max = 200) String email,
      @Size(max = 30) String phone,
      @Size(max = 250) String address,
      @NotNull Boolean active,
      @NotNull @PositiveOrZero Long version) {
    ThirdPartyUseCases.Values values() {
      return new ThirdPartyUseCases.Values(
          documentType,
          documentNumber,
          verificationDigit,
          name,
          category,
          email,
          phone,
          address,
          active,
          version);
    }
  }

  @GetMapping
  public Page<ThirdParty> list(
      Authentication auth,
      @PathVariable UUID companyId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return service.list(CurrentActor.from(auth), companyId, new SearchPage(q, page, size));
  }

  @GetMapping("/{id}")
  public ThirdParty get(Authentication auth, @PathVariable UUID companyId, @PathVariable UUID id) {
    return service.get(CurrentActor.from(auth), companyId, id);
  }

  @PostMapping
  public ResponseEntity<ThirdParty> create(
      Authentication auth, @PathVariable UUID companyId, @Valid @RequestBody Request request) {
    var party = service.create(CurrentActor.from(auth), companyId, request.values());
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/third-parties/" + party.id()))
        .body(party);
  }

  @PutMapping("/{id}")
  public ThirdParty update(
      Authentication auth,
      @PathVariable UUID companyId,
      @PathVariable UUID id,
      @Valid @RequestBody Request request) {
    return service.update(CurrentActor.from(auth), companyId, id, request.values());
  }
}
