package com.worksoft.company.adapter.in.web;


import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import com.worksoft.company.domain.accounting.Account;
import com.worksoft.company.application.port.in.*;
@RestController
@Profile("postgres")
@RequestMapping("/api/v1/companies/{companyId}/accounts")
public class AccountController {
    private final AccountUseCases service;
    public AccountController(AccountUseCases service){this.service=service;}
    public record Request(@NotBlank @Pattern(regexp="[1-9][0-9]*") @Size(max=10) String code,
        @NotBlank @Size(max=200) String name,@NotNull Account.Nature nature,@NotNull Boolean active,
        @NotNull Boolean allowsPosting,@Size(max=1000) String notes,@PositiveOrZero long version) {
        AccountUseCases.Values values(){return new AccountUseCases.Values(code,name,nature,active,allowsPosting,notes,version);}
    }
    private Actor actor(Authentication auth){
        if(auth.getPrincipal() instanceof Actor local)return local;
        var jwt=(Jwt)auth.getPrincipal();
        return new Actor(jwt.getIssuer().toString(),jwt.getSubject(),false);
    }
    @GetMapping public AccountPage list(Authentication auth,@PathVariable UUID companyId,@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.list(actor(auth),companyId,q,page,size);}
    @GetMapping("/{id}") public Account get(Authentication auth,@PathVariable UUID companyId,@PathVariable UUID id){return service.get(actor(auth),companyId,id);}
    @PostMapping public ResponseEntity<Account> create(Authentication auth,@PathVariable UUID companyId,@Valid @RequestBody Request request){
        var account=service.create(actor(auth),companyId,request.values());
        return ResponseEntity.created(URI.create("/api/v1/companies/"+companyId+"/accounts/"+account.id())).body(account);
    }
    @PutMapping("/{id}") public Account update(Authentication auth,@PathVariable UUID companyId,@PathVariable UUID id,@Valid @RequestBody Request request){
        return service.update(actor(auth),companyId,id,request.values());
    }
}
