package com.worksoft.company.adapter.out.persistence;

import static com.worksoft.company.domain.exception.BusinessException.Code.COMPANY_NOT_FOUND;

import com.worksoft.company.domain.exception.BusinessException;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcCompanyLock {
  private final JdbcTemplate jdbc;

  JdbcCompanyLock(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  void acquire(UUID company) {
    jdbc.execute("SET LOCAL lock_timeout='5s'");
    if (jdbc.query(
            "SELECT id FROM companies WHERE id=? FOR UPDATE",
            (rs, n) -> rs.getObject(1, UUID.class),
            company)
        .isEmpty()) throw new BusinessException(COMPANY_NOT_FOUND);
  }
}
