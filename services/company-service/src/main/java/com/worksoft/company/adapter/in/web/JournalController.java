package com.worksoft.company.adapter.in.web;

import com.worksoft.company.application.port.in.JournalUseCases;
import com.worksoft.company.application.port.in.Page;
import com.worksoft.company.application.port.in.PostingOptions;
import com.worksoft.company.application.port.in.SearchPage;
import com.worksoft.company.domain.journal.JournalHeader;
import com.worksoft.company.domain.journal.JournalLine;
import com.worksoft.company.domain.journal.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("postgres")
@RequestMapping("/api/v1/companies/{companyId}/journals")
public class JournalController {
  private final JournalUseCases service;

  public JournalController(JournalUseCases service) {
    this.service = service;
  }

  public record Header(
      @NotNull LocalDate date,
      @NotNull JournalHeader.Type type,
      @NotBlank @Size(max = 200) String origin,
      @NotBlank @Size(max = 1000) String description,
      @NotBlank @Size(max = 500) String supportReference,
      @NotBlank @Size(max = 100) String preparedBy) {
    JournalHeader value() {
      return new JournalHeader(date, type, origin, description, supportReference, preparedBy);
    }
  }

  public record Line(
      @NotNull UUID accountId,
      @NotNull UUID partyId,
      @Size(max = 300) String description,
      @NotBlank String debit,
      @NotBlank String credit) {
    JournalLine value() {
      return new JournalLine(
          accountId, partyId, description, Money.parse(debit), Money.parse(credit));
    }
  }

  public record Request(
      @NotNull @Valid Header header,
      @NotNull @Size(min = 1, max = 200) List<@NotNull @Valid Line> lines,
      @NotNull @PositiveOrZero Long version) {
    JournalUseCases.Values values() {
      return new JournalUseCases.Values(
          header.value(), lines.stream().map(Line::value).toList(), version);
    }
  }

  public record PostRequest(@NotNull @PositiveOrZero Long version) {}

  @GetMapping
  public Page<JournalResponse.Summary> list(
      Authentication auth,
      @PathVariable UUID companyId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    var result = service.list(CurrentActor.from(auth), companyId, new SearchPage(q, page, size));
    return new Page<>(
        result.items().stream().map(JournalResponse::summary).toList(),
        result.total(),
        result.page(),
        result.size());
  }

  @GetMapping("/options")
  public PostingOptions options(
      Authentication auth,
      @PathVariable UUID companyId,
      @RequestParam(defaultValue = "") String accountQuery,
      @RequestParam(defaultValue = "") String partyQuery) {
    return service.options(CurrentActor.from(auth), companyId, accountQuery, partyQuery);
  }

  @GetMapping("/{id}")
  public JournalResponse get(
      Authentication auth, @PathVariable UUID companyId, @PathVariable UUID id) {
    return JournalResponse.from(service.get(CurrentActor.from(auth), companyId, id));
  }

  @PostMapping
  public ResponseEntity<JournalResponse> create(
      Authentication auth,
      @PathVariable UUID companyId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody Request request) {
    var document = service.create(CurrentActor.from(auth), companyId, request.values(), key);
    return ResponseEntity.created(
            URI.create("/api/v1/companies/" + companyId + "/journals/" + document.draft().id()))
        .body(JournalResponse.from(document));
  }

  @PutMapping("/{id}")
  public JournalResponse update(
      Authentication auth,
      @PathVariable UUID companyId,
      @PathVariable UUID id,
      @Valid @RequestBody Request request) {
    return JournalResponse.from(
        service.update(CurrentActor.from(auth), companyId, id, request.values()));
  }

  @PostMapping("/{id}/post")
  public JournalResponse post(
      Authentication auth,
      @PathVariable UUID companyId,
      @PathVariable UUID id,
      @Valid @RequestBody PostRequest request) {
    return JournalResponse.from(
        service.post(CurrentActor.from(auth), companyId, id, request.version()));
  }
}
