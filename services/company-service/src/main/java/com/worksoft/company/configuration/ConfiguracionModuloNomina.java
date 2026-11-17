package com.worksoft.company.configuration;

import com.worksoft.company.adapter.out.persistence.PostgresAlmacenNomina;
import com.worksoft.company.application.port.in.CasosUsoNomina;
import com.worksoft.company.application.port.out.*;
import com.worksoft.company.application.service.*;
import com.worksoft.company.domain.nomina.MotorNominaOrdinaria;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@Profile("postgres")
public class ConfiguracionModuloNomina {
  @Bean
  AlmacenNomina almacenNomina(
      JdbcTemplate jdbc, PlatformTransactionManager gestor, JsonMapper json) {
    return new PostgresAlmacenNomina(jdbc, new TransactionTemplate(gestor), json);
  }

  @Bean
  CasosUsoNomina casosUsoNomina(
      CompanyAccess acceso, AlmacenNomina almacen, ThirdPartyStore terceros, Clock reloj) {
    return new ServicioNomina(acceso, almacen, terceros, reloj, new MotorNominaOrdinaria());
  }
}
