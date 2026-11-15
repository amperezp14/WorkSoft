package com.worksoft.company.configuration;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;
import com.worksoft.company.adapter.out.persistence.InMemoryCompanyStore;
import com.worksoft.company.adapter.out.cursor.SignedCursorCodec;
import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.out.*;
@Configuration
@Profile("local")
public class LocalConfiguration {
    @Bean CompanyStore memoryStore(Clock clock) { return new InMemoryCompanyStore(clock); }
    @Bean CursorCodec localCursors(Clock clock) { return new SignedCursorCodec(UUID.randomUUID().toString(), clock); }
    @Bean SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/v1/companies", "/api/v1/companies/**", "/actuator/health", "/actuator/health/**").permitAll().anyRequest().denyAll())
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        new Actor("urn:worksoft:local", "workspace", true), null,
                        List.of(new SimpleGrantedAuthority("company:provision"))));
                    SecurityContextHolder.setContext(context);
                    try { chain.doFilter(request, response); } finally { SecurityContextHolder.clearContext(); }
                }
            }, AnonymousAuthenticationFilter.class).build();
    }
}
