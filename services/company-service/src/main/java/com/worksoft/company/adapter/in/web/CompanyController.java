package com.worksoft.company.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.worksoft.company.application.port.in.*;
import com.worksoft.company.domain.model.CompanyId;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CreateCompanyUseCase create;
    private final QueryCompaniesUseCase queries;
    public CompanyController(CreateCompanyUseCase create, QueryCompaniesUseCase queries) {
        this.create=create; this.queries=queries;
    }
    public record CreateRequest(@NotBlank @Size(max=200) String legalName,
            @NotNull @Pattern(regexp="[0-9]{1,15}") String nit,
            @NotNull @Pattern(regexp="[0-9]") String verificationDigit) { }
    public record PageResponse(List<CompanyResponse> items, String nextCursor) { }
    private Actor actor(Jwt jwt, Authentication auth) {
        boolean provision = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("company:provision"));
        return new Actor(jwt.getIssuer().toString(), jwt.getSubject(), provision);
    }
    @PostMapping
    public ResponseEntity<CompanyResponse> create(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody CreateRequest request) {
        var company=create.create(actor(jwt,auth),new CreateCompanyCommand(request.legalName(),request.nit(),request.verificationDigit(),key));
        return ResponseEntity.created(URI.create("/api/v1/companies/"+company.id().value())).body(CompanyResponse.from(company));
    }
    @GetMapping("/{companyId}")
    public CompanyResponse get(@AuthenticationPrincipal Jwt jwt, Authentication auth, @PathVariable UUID companyId) {
        return CompanyResponse.from(queries.get(actor(jwt,auth),new CompanyId(companyId)));
    }
    @GetMapping
    public PageResponse list(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @RequestParam(defaultValue="20") int limit, @RequestParam(required=false) String cursor) {
        var page=queries.list(actor(jwt,auth),limit,cursor);
        return new PageResponse(page.items().stream().map(CompanyResponse::from).toList(),page.nextCursor());
    }
}
