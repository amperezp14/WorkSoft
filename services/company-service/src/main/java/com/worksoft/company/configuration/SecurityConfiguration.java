package com.worksoft.company.configuration;

import java.io.IOException;
import java.util.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class SecurityConfiguration {
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        var converter=new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object realmAccess = jwt.getClaims().get("realm_access");
            Object roles = realmAccess instanceof Map<?,?> map ? map.get("roles") : List.of();
            if (!(roles instanceof Collection<?> values)) return List.of();
            return values.stream().filter(String.class::isInstance).map(String.class::cast)
                    .filter("company:provision"::equals).map(SimpleGrantedAuthority::new)
                    .map(a -> (org.springframework.security.core.GrantedAuthority)a).toList();
        });
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/companies").hasAuthority("company:provision")
                .requestMatchers("/api/v1/companies", "/api/v1/companies/**").authenticated().anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,ex) -> unauthorized(res)))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> unauthorized(res))
                .accessDeniedHandler((req,res,ex) -> writeError(res,403,"FORBIDDEN","Operación no permitida")))
            .build();
    }
    private static void unauthorized(HttpServletResponse res) throws IOException {
        res.setHeader("WWW-Authenticate","Bearer");writeError(res,401,"UNAUTHORIZED","Autenticación requerida");
    }
    private static void writeError(HttpServletResponse res,int status,String code,String title) throws IOException {
        res.setStatus(status);res.setContentType("application/problem+json");res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"type\":\"about:blank\",\"status\":"+status+",\"code\":\""+code+"\",\"title\":\""+title+"\"}");
    }
    public static OAuth2TokenValidator<Jwt> validators(String issuer,String audience) {
        OAuth2TokenValidator<Jwt> audienceAndAccessToken=jwt -> {
            if(jwt.getAudience()!=null && jwt.getAudience().contains(audience) && jwt.getExpiresAt()!=null
                    && "Bearer".equals(jwt.getClaimAsString("typ"))
                    && jwt.getSubject()!=null && !jwt.getSubject().isBlank()) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),audienceAndAccessToken);
    }
    @Bean JwtDecoder jwtDecoder(@Value("${worksoft.security.issuer}") String issuer,
            @Value("${worksoft.security.jwk-set-uri}") String jwks,
            @Value("${worksoft.security.audience}") String audience) {
        var decoder=NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(validators(issuer,audience));return decoder;
    }
}
