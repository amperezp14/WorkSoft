package com.worksoft.company;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.worksoft.company.adapter.out.cursor.SignedCursorCodec;
import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.out.PagePosition;
import com.worksoft.company.domain.exception.BusinessException;
import static org.assertj.core.api.Assertions.*;

class CursorTests {
    @Test void rejectsExpiredCursor() {
        String secret="test-only-secret-with-at-least-32-bytes";
        var actor=new Actor("issuer","subject",false);
        var at=Instant.parse("2026-11-08T00:00:00Z");
        var encoder=new SignedCursorCodec(secret,Clock.fixed(at,ZoneOffset.UTC));
        var decoder=new SignedCursorCodec(secret,Clock.fixed(at.plusSeconds(901),ZoneOffset.UTC));
        String cursor=encoder.encode(new PagePosition(at,UUID.randomUUID()),actor,20);
        assertThatThrownBy(()->decoder.decode(cursor,actor,20)).isInstanceOf(BusinessException.class);
    }
}
