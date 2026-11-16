package com.worksoft.company.adapter.in.web;

import com.worksoft.company.application.port.in.Actor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

final class CurrentActor {
  private CurrentActor() {}

  static Actor from(Authentication authentication) {
    if (authentication.getPrincipal() instanceof Actor actor) return actor;
    var jwt = (Jwt) authentication.getPrincipal();
    boolean provision =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("company:provision"));
    return new Actor(jwt.getIssuer().toString(), jwt.getSubject(), provision);
  }
}
