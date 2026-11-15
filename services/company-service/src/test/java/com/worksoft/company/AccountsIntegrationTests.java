package com.worksoft.company;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import com.jayway.jsonpath.JsonPath;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.worksoft.company.application.port.in.*;
import com.worksoft.company.domain.accounting.Account;
import com.worksoft.company.domain.exception.BusinessException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
class AccountsIntegrationTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17.6");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p){
        p.add("spring.datasource.url",PG::getJdbcUrl);p.add("spring.datasource.username",PG::getUsername);p.add("spring.datasource.password",PG::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationContext context;
    @Autowired AccountUseCases service;
    private String company;
    @BeforeEach void setup() throws Exception {
        jdbc.execute("TRUNCATE companies CASCADE");
        company=createCompany("100");
    }
    private String createCompany(String nit) throws Exception {
        var response=mvc.perform(post("/api/v1/companies").header("Idempotency-Key",UUID.randomUUID().toString())
            .contentType("application/json").content("{\"legalName\":\"Ejemplo\",\"nit\":\""+nit+"\",\"verificationDigit\":\"0\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response,"$.id");
    }
    private String url(){return "/api/v1/companies/"+company+"/accounts";}
    private String body(String code,String name,long version){
        return "{\"code\":\""+code+"\",\"name\":\""+name+"\",\"nature\":\"DEBIT\",\"active\":true,\"allowsPosting\":true,\"notes\":\"Prueba\",\"version\":"+version+"}";
    }
    private String create(String code) throws Exception {
        return mvc.perform(post(url()).contentType("application/json").content(body(code,"Auxiliar",0)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
    @Test void seedsReferenceAndCompanyWithoutIdentityProvider() throws Exception {
        assertThat(context.getBeansOfType(JwtDecoder.class)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM puc_reference_accounts WHERE name LIKE '%<%'",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM puc_reference_accounts",Integer.class)).isEqualTo(108);
        mvc.perform(get(url()).param("q","110505")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].name").value("CAJA GENERAL"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chart_accounts WHERE company_id=?",Integer.class,UUID.fromString(company))).isEqualTo(108);
    }
    @Test void localPostgresWorkspaceListsExistingCompaniesWithoutIdentityMembership() throws Exception {
        jdbc.update("DELETE FROM memberships WHERE company_id=?",UUID.fromString(company));
        mvc.perform(get("/api/v1/companies")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(company));
        mvc.perform(get(url())).andExpect(status().isOk());
    }
    @Test void createsUpdatesAndStoresVersionInPostgres() throws Exception {
        String saved=create("11050501");String id=JsonPath.read(saved,"$.id");
        mvc.perform(put(url()+"/"+id).contentType("application/json").content(body("11050501","Caja punto norte",0)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.name").value("Caja punto norte"));
        mvc.perform(get(url()+"/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Caja punto norte"));
        assertThat(jdbc.queryForObject("SELECT name FROM chart_accounts WHERE company_id=? AND code='11050501'",String.class,UUID.fromString(company))).isEqualTo("Caja punto norte");
        mvc.perform(put(url()+"/"+id).contentType("application/json").content(body("11050501","Edición vieja",0)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACCOUNT_VERSION_CONFLICT"));
        mvc.perform(put(url()+"/"+id).contentType("application/json").content(body("11050502","Cambiar código",1))).andExpect(status().isBadRequest());
    }
    @Test void rejectsDuplicateAndInvalidHierarchyWithoutPartialWrites() throws Exception {
        create("11050501");
        mvc.perform(post(url()).contentType("application/json").content(body("11050501","Duplicado",0))).andExpect(status().isConflict());
        mvc.perform(post(url()).contentType("application/json").content(body("99999901","Sin padre",0)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACCOUNT_HIERARCHY_CONFLICT"));
        mvc.perform(post(url()).contentType("application/json").content(body("1105050101","Hija de movimiento",0))).andExpect(status().isConflict());
        mvc.perform(post(url()).contentType("application/json").content(body("123","Longitud incorrecta",0))).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chart_accounts WHERE company_id=?",Integer.class,UUID.fromString(company))).isEqualTo(109);
    }
    @Test void enforcesCompanyScopeAndAllowsSameCodeInDifferentCompanies() throws Exception {
        String saved=create("11050501");String id=JsonPath.read(saved,"$.id");
        String firstCompany=company;company=createCompany("200");
        mvc.perform(get(url()+"/"+id)).andExpect(status().isNotFound());
        mvc.perform(put(url()+"/"+id).contentType("application/json").content(body("11050501","Otra empresa",0))).andExpect(status().isNotFound());
        create("11050501");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM chart_accounts WHERE code='11050501'",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT name FROM chart_accounts WHERE id=? AND company_id=?",String.class,UUID.fromString(id),UUID.fromString(firstCompany))).isEqualTo("Auxiliar");
    }
    @Test void paginatesAndValidatesParametersAndTextTypes() throws Exception {
        mvc.perform(get(url()).param("size","100")).andExpect(jsonPath("$.items.length()").value(100));
        mvc.perform(get(url()).param("size","100").param("page","1")).andExpect(jsonPath("$.items.length()").value(8));
        mvc.perform(get(url()).param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get(url()).param("q","%")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(post(url()).contentType("application/json").content(body("11050501","Auxiliar",0).replace("\"11050501\"","11050501"))).andExpect(status().isBadRequest());
    }
    @Test void preventsDeactivatingParentWithActiveChildren() throws Exception {
        String id=jdbc.queryForObject("SELECT id::text FROM chart_accounts WHERE company_id=? AND code='1105'",String.class,UUID.fromString(company));
        mvc.perform(put(url()+"/"+id).contentType("application/json").content(body("1105","Caja",0).replace("\"active\":true","\"active\":false").replace("\"allowsPosting\":true","\"allowsPosting\":false")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACCOUNT_HIERARCHY_CONFLICT"));
    }
    @Test void serializesConcurrentCreationOfSameCode() throws Exception {
        var actor=new Actor("urn:worksoft:local","workspace",true);
        UUID companyId=UUID.fromString(company);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<String> task=()->{
                start.await();
                try{service.create(actor,companyId,new AccountUseCases.Values("11050501","Auxiliar",Account.Nature.DEBIT,true,true,"",0));return "CREATED";}
                catch(BusinessException ex){return ex.code().name();}
            };
            var a=pool.submit(task);var b=pool.submit(task);start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder("CREATED","ACCOUNT_CONFLICT");
        }
    }
}
