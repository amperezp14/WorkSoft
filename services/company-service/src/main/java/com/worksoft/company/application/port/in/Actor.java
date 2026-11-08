package com.worksoft.company.application.port.in;

import java.util.Objects;

/** Solo el adaptador de seguridad puede construir el actor de una petición HTTP. */
public record Actor(String issuer, String subject, boolean canProvision) {
    public Actor { Objects.requireNonNull(issuer); Objects.requireNonNull(subject); }
}
