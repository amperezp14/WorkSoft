package com.worksoft.company.configuration;

import com.worksoft.company.adapter.out.persistence.PostgresJournalStore;
import com.worksoft.company.adapter.out.persistence.PostgresThirdPartyStore;
import com.worksoft.company.application.port.out.CompanyStore;
import com.worksoft.company.application.port.out.JournalStore;
import com.worksoft.company.application.port.out.ThirdPartyStore;
import com.worksoft.company.application.service.CompanyAccess;
import com.worksoft.company.application.service.JournalService;
import com.worksoft.company.application.service.ThirdPartyService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("postgres")
public class JournalConfiguration {
  @Bean
  CompanyAccess companyAccess(CompanyStore companies) {
    return new CompanyAccess(companies);
  }

  @Bean
  ThirdPartyStore thirdPartyStore(JdbcTemplate jdbc, PlatformTransactionManager manager) {
    return new PostgresThirdPartyStore(jdbc, new TransactionTemplate(manager));
  }

  @Bean
  JournalStore journalStore(JdbcTemplate jdbc, PlatformTransactionManager manager) {
    return new PostgresJournalStore(jdbc, new TransactionTemplate(manager));
  }

  @Bean
  ThirdPartyService thirdPartyService(CompanyAccess access, ThirdPartyStore store, Clock clock) {
    return new ThirdPartyService(access, store, clock);
  }

  @Bean
  JournalService journalService(CompanyAccess access, JournalStore store, Clock clock) {
    return new JournalService(access, store, clock);
  }
}
