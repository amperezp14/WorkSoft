package com.worksoft.company.configuration;

import com.worksoft.company.adapter.out.persistence.*;
import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.application.service.*;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Profile("postgres")
public class ConfiguracionModuloFacturacion {
  @Bean
  AlmacenFacturacion almacenFacturacion(
      JdbcTemplate jdbc, PlatformTransactionManager transacciones, JsonMapper json) {
    return new PostgresAlmacenFacturacion(jdbc, new TransactionTemplate(transacciones), json);
  }

  @Bean
  ContabilidadFacturacion contabilidadFacturacion(JournalService comprobantes) {
    return new AdaptadorContabilidadFacturacion(comprobantes);
  }

  @Bean
  CasosUsoFacturacion facturacionUseCases(
      CompanyAccess acceso,
      AlmacenFacturacion almacen,
      ContabilidadFacturacion contabilidad,
      Clock reloj) {
    return new ServicioFacturacion(acceso, almacen, contabilidad, reloj);
  }
}
