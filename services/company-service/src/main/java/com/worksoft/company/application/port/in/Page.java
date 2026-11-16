package com.worksoft.company.application.port.in;

import java.util.List;

public record Page<T>(List<T> items, long total, int page, int size) {
  public Page {
    items = List.copyOf(items);
  }
}
