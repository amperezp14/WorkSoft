package com.worksoft.company.configuration;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.worksoft.company.adapter.out.persistence.PostgresCompanyStore;
import com.worksoft.company.adapter.out.cursor.SignedCursorCodec;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.application.service.CompanyService;

@Configuration
public class CompanyConfiguration {
    @Bean org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer strictTextInput() {
        return builder -> builder.withCoercionConfig(tools.jackson.databind.type.LogicalType.Textual, config -> {
            config.setCoercion(tools.jackson.databind.cfg.CoercionInputShape.Integer, tools.jackson.databind.cfg.CoercionAction.Fail);
            config.setCoercion(tools.jackson.databind.cfg.CoercionInputShape.Float, tools.jackson.databind.cfg.CoercionAction.Fail);
            config.setCoercion(tools.jackson.databind.cfg.CoercionInputShape.Boolean, tools.jackson.databind.cfg.CoercionAction.Fail);
        });
    }
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean CompanyStore companyStore(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
        return new PostgresCompanyStore(jdbc,new TransactionTemplate(manager),clock);
    }
    @Bean CursorCodec cursorCodec(@Value("${worksoft.cursor-secret}") String secret,Clock clock) {
        return new SignedCursorCodec(secret,clock);
    }
    @Bean CompanyService companyService(CompanyStore store,CursorCodec cursors,Clock clock) {
        return new CompanyService(store,cursors,clock);
    }
}
