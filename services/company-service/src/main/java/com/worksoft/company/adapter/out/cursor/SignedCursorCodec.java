package com.worksoft.company.adapter.out.cursor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.domain.exception.BusinessException;
import static com.worksoft.company.domain.exception.BusinessException.Code.VALIDATION_ERROR;

public final class SignedCursorCodec implements CursorCodec {
    private final byte[] secret;
    private final Clock clock;
    public SignedCursorCodec(String secret, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8); this.clock = clock;
        if (this.secret.length < 32) throw new IllegalArgumentException("CURSOR_SECRET debe tener al menos 32 bytes");
    }
    private String actorTag(Actor actor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sign(actor.issuer().length()+":"+actor.issuer()+actor.subject()));
    }
    @Override public String encode(PagePosition pos, Actor actor, int limit) {
        String body = pos.createdAt()+"|"+pos.id()+"|"+clock.instant().plus(Duration.ofMinutes(15)).getEpochSecond()+"|"+limit+"|"+actorTag(actor);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(body.getBytes(StandardCharsets.UTF_8))+"."+
                Base64.getUrlEncoder().withoutPadding().encodeToString(sign(body));
    }
    @Override public PagePosition decode(String cursor, Actor actor, int limit) {
        try {
            if (cursor.isEmpty() || cursor.length()>2048) throw new IllegalArgumentException();
            String[] parts = cursor.split("\\.",-1);
            if (parts.length!=2) throw new IllegalArgumentException();
            String body = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(body), Base64.getUrlDecoder().decode(parts[1]))) throw new IllegalArgumentException();
            String[] fields = body.split("\\|",-1);
            if (fields.length!=5 || Long.parseLong(fields[2])<=clock.instant().getEpochSecond()
                    || Integer.parseInt(fields[3])!=limit || !fields[4].equals(actorTag(actor))) throw new IllegalArgumentException();
            return new PagePosition(Instant.parse(fields[0]), UUID.fromString(fields[1]));
        } catch (RuntimeException ex) { throw new BusinessException(VALIDATION_ERROR); }
    }
    private byte[] sign(String body) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException ex) { throw new IllegalStateException(ex); }
    }
}
