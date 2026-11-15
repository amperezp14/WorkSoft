package com.worksoft.company.configuration;


import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.worksoft.company.adapter.out.persistence.PostgresAccountStore;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.application.service.AccountService;
@Configuration
@Profile("postgres")
public class AccountingConfiguration {
    @Bean AccountStore accountStore(JdbcTemplate jdbc,PlatformTransactionManager manager){return new PostgresAccountStore(jdbc,new TransactionTemplate(manager));}
    @Bean AccountService accountService(CompanyStore companies,AccountStore accounts,Clock clock){return new AccountService(companies,accounts,clock);}
}
