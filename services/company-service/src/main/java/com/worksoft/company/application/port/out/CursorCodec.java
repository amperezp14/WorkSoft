package com.worksoft.company.application.port.out;

import com.worksoft.company.application.port.in.Actor;

public interface CursorCodec {
    PagePosition decode(String cursor, Actor actor, int limit);
    String encode(PagePosition position, Actor actor, int limit);
}
