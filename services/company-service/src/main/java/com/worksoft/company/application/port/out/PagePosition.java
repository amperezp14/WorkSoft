package com.worksoft.company.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record PagePosition(Instant createdAt, UUID id) { }
