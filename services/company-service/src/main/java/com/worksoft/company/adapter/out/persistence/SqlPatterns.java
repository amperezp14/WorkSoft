package com.worksoft.company.adapter.out.persistence;

final class SqlPatterns {
  private SqlPatterns() {}

  static String contains(String value) {
    return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}
