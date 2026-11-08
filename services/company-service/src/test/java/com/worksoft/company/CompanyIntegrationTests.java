package com.worksoft.company;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.worksoft.company.application.port.in.*;
import com.worksoft.company.application.service.CompanyService;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CompanyIntegrationTests {
    private static final String ISSUER="https://issuer.worksoft.test";
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17.6");
    static final RSAKey KEY;
    static final HttpServer JWKS;
    static {
        try {
            KEY=new RSAKeyGenerator(2048).keyID("integration-key").generate();
            JWKS=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            JWKS.createContext("/jwks",exchange -> {
                byte[] body=new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type","application/json");
                exchange.sendResponseHeaders(200,body.length);
                try(var out=exchange.getResponseBody()){out.write(body);}
            });
            JWKS.start();
        } catch(Exception ex){throw new ExceptionInInitializerError(ex);}
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p){
        p.add("spring.datasource.url",PG::getJdbcUrl);
        p.add("spring.datasource.username",PG::getUsername);
        p.add("spring.datasource.password",PG::getPassword);
        p.add("worksoft.cursor-secret",()->"integration-test-only-cursor-key-32-bytes");
        p.add("worksoft.security.issuer",()->ISSUER);
        p.add("worksoft.security.jwk-set-uri",()->"http://127.0.0.1:"+JWKS.getAddress().getPort()+"/jwks");
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CompanyService service;
    @BeforeEach void clean(){jdbc.execute("TRUNCATE idempotency,audit,outbox,memberships,companies CASCADE");}
    @AfterAll static void stopJwks(){JWKS.stop(0);}
    private String token(String user,boolean provision) throws Exception {
        return token(user,provision,ISSUER,"company-service","Bearer",Instant.now().plusSeconds(300),KEY);
    }
    private String token(String user,boolean provision,String issuer,String audience,String typ,Instant expires,RSAKey key) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(issuer).subject(user).audience(audience)
            .issueTime(Date.from(Instant.now().minusSeconds(10))).expirationTime(Date.from(expires))
            .claim("typ",typ).claim("realm_access",Map.of("roles",provision?List.of("company:provision"):List.of())).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("integration-key").build(),claims);
        jwt.sign(new RSASSASigner(key));return jwt.serialize();
    }
    private String body(String nit){return "{\"legalName\":\" Empresa sintética \",\"nit\":\""+nit+"\",\"verificationDigit\":\"0\"}";}
    private MvcResult create(String user,String key,String nit) throws Exception {
        return mvc.perform(post("/api/v1/companies").header("Authorization","Bearer "+token(user,true))
            .header("Idempotency-Key",key).contentType("application/json").content(body(nit)))
            .andExpect(status().isCreated()).andReturn();
    }
    private String id(MvcResult result) throws Exception {return JsonPath.read(result.getResponse().getContentAsString(),"$.id");}
    private long count(String table){return jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class);}

    @Test void createsCompanyAndAllAtomicRecords() throws Exception {
        var response=create("alice","creation-key-0001","000000001");
        assertThat(response.getResponse().getHeader("Location")).isEqualTo("/api/v1/companies/"+id(response));
        String content=response.getResponse().getContentAsString();
        assertThat((String)JsonPath.read(content,"$.legalName")).isEqualTo("Empresa sintética");
        assertThat((String)JsonPath.read(content,"$.nit")).isEqualTo("000000001");
        assertThat((String)JsonPath.read(content,"$.currency")).isEqualTo("COP");
        assertThat((String)JsonPath.read(content,"$.timeZone")).isEqualTo("America/Bogota");
        for(String table:List.of("companies","memberships","idempotency","audit","outbox")) assertThat(count(table)).isEqualTo(1);
        String payload=jdbc.queryForObject("SELECT payload::text FROM outbox",String.class);
        assertThat(payload).doesNotContain("Empresa sintética","000000001","alice");
        assertThat((String)JsonPath.read(payload,"$.data.companyId")).isEqualTo(id(response));
        mvc.perform(get("/api/v1/companies/"+id(response)).header("Authorization","Bearer "+token("alice",false)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id(response)));
    }
    @Test void rejectsMissingTokenAndInsufficientPrivilege() throws Exception {
        mvc.perform(get("/api/v1/companies")).andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate","Bearer")).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(post("/api/v1/companies").header("Authorization","Bearer "+token("reader",false))
            .header("Idempotency-Key","creation-key-0001").contentType("application/json").content(body("1")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(count("companies")).isZero();
    }
    @Test void verifiesAudienceIssuerExpirationSignatureAndTokenType() throws Exception {
        var future=Instant.now().plusSeconds(300);
        var other=new RSAKeyGenerator(2048).generate();
        var tokens=List.of(token("a",true,ISSUER,"wrong","Bearer",future,KEY),
            token("a",true,"https://other.test","company-service","Bearer",future,KEY),
            token("a",true,ISSUER,"company-service","Bearer",Instant.now().minusSeconds(300),KEY),
            token("a",true,ISSUER,"company-service","ID",future,KEY),
            token("a",true,ISSUER,"company-service","Bearer",future,other));
        for(String value:tokens) mvc.perform(get("/api/v1/companies").header("Authorization","Bearer "+value))
            .andExpect(status().isUnauthorized());
    }
    @Test void sameKeyReplaysIdenticalRepresentationWithoutSideEffects() throws Exception {
        var first=create("alice","creation-key-0001","1");
        var second=create("alice","creation-key-0001","1");
        assertThat(second.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertThat(second.getResponse().getHeader("Location")).isEqualTo(first.getResponse().getHeader("Location"));
        assertThat(count("outbox")).isEqualTo(1);assertThat(count("audit")).isEqualTo(1);
    }
    @Test void keyWithDifferentPayloadConflicts() throws Exception {
        create("alice","creation-key-0001","1");
        mvc.perform(post("/api/v1/companies").header("Authorization","Bearer "+token("alice",true))
            .header("Idempotency-Key","creation-key-0001").contentType("application/json").content(body("2")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        assertThat(count("companies")).isEqualTo(1);
    }
    @Test void duplicateNitConflictsAndDoesNotLeakExistingCompany() throws Exception {
        create("alice","creation-key-0001","1");
        var result=mvc.perform(post("/api/v1/companies").header("Authorization","Bearer "+token("bob",true))
            .header("Idempotency-Key","creation-key-0002").contentType("application/json").content(body("1")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COMPANY_CONFLICT")).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("alice","Empresa sintética");
        assertThat(count("memberships")).isEqualTo(1);
    }
    @Test void hidesOtherCompaniesAndRevokedMemberships() throws Exception {
        String company=id(create("alice","creation-key-0001","1"));
        mvc.perform(get("/api/v1/companies/"+company).header("Authorization","Bearer "+token("bob",true)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
        mvc.perform(get("/api/v1/companies/"+UUID.randomUUID()).header("Authorization","Bearer "+token("bob",true)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COMPANY_NOT_FOUND"));
        jdbc.update("UPDATE memberships SET active=false");
        mvc.perform(get("/api/v1/companies/"+company).header("Authorization","Bearer "+token("alice",true)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/companies").header("Authorization","Bearer "+token("alice",true)))
            .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(post("/api/v1/companies").header("Authorization","Bearer "+token("alice",true))
            .header("Idempotency-Key","creation-key-0001").contentType("application/json").content(body("1")))
            .andExpect(status().isForbidden());
    }
    @Test void paginatesAndBindsCursorToActorAndLimit() throws Exception {
        create("alice","creation-key-0001","1");create("alice","creation-key-0002","2");
        var page=mvc.perform(get("/api/v1/companies?limit=1").header("Authorization","Bearer "+token("alice",false)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn();
        String cursor=JsonPath.read(page.getResponse().getContentAsString(),"$.nextCursor");
        mvc.perform(get("/api/v1/companies").param("limit","1").param("cursor",cursor)
            .header("Authorization","Bearer "+token("alice",false))).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").doesNotExist());
        mvc.perform(get("/api/v1/companies").param("limit","1").param("cursor",cursor)
            .header("Authorization","Bearer "+token("bob",false))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/companies").param("limit","2").param("cursor",cursor)
            .header("Authorization","Bearer "+token("alice",false))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/companies").param("limit","1").param("cursor",cursor+"tampered")
            .header("Authorization","Bearer "+token("alice",false))).andExpect(status().isBadRequest());
    }
    @Test void validatesUnknownFieldsScalarTypesAndMalformedInputs() throws Exception {
        var invalid=List.of(body("1").replace("Empresa sintética","   "),body("1").replace("\"nit\":\"1\"","\"nit\":1"),
            body("1").replace("\"0\"","\"00\""),body("1").replace("}",",\"country\":\"US\"}"),"{}");
        for(String value:invalid) mvc.perform(post("/api/v1/companies").header("Authorization","Bearer "+token("alice",true))
            .header("Idempotency-Key","creation-key-0001").contentType("application/json").content(value))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/companies/not-uuid").header("Authorization","Bearer "+token("alice",true)))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/companies?limit=101").header("Authorization","Bearer "+token("alice",true)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").exists());
        assertThat(count("companies")).isZero();
    }
    @Test void rollsBackAllWritesIfOutboxFails() {
        jdbc.execute("ALTER TABLE outbox ADD CONSTRAINT simulate_failure CHECK (false)");
        try {
            assertThatThrownBy(()->service.create(new Actor(ISSUER,"alice",true),
                new CreateCompanyCommand("Ejemplo","1","0","creation-key-0001"))).isInstanceOf(RuntimeException.class);
            for(String table:List.of("companies","memberships","idempotency","audit","outbox")) assertThat(count(table)).isZero();
        } finally {jdbc.execute("ALTER TABLE outbox DROP CONSTRAINT simulate_failure");}
    }
    @Test void rejectsTokenWithoutExpiration() throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(ISSUER).subject("alice").audience("company-service")
            .claim("typ","Bearer").build();
        var signed=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("integration-key").build(),claims);
        signed.sign(new RSASSASigner(KEY));
        mvc.perform(get("/api/v1/companies").header("Authorization","Bearer "+signed.serialize()))
            .andExpect(status().isUnauthorized());
    }
    @Test void readerCanReadButCompanyAdminDoesNotImplyPlatformPrivilege() throws Exception {
        String company=id(create("alice","creation-key-0001","1"));
        jdbc.update("INSERT INTO memberships(company_id,issuer,subject,role,active) VALUES (?,?,?,'READER',true)",
            UUID.fromString(company),ISSUER,"reader");
        mvc.perform(get("/api/v1/companies/"+company).header("Authorization","Bearer "+token("reader",false)))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/companies").header("Authorization","Bearer "+token("alice",false))
            .header("Idempotency-Key","creation-key-0002").contentType("application/json").content(body("2")))
            .andExpect(status().isForbidden());
    }
    @Test void concurrentDifferentKeysCannotDuplicateNit() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<String> first=()->createConcurrent(start,"concurrent-key-001");
            Callable<String> second=()->createConcurrent(start,"concurrent-key-002");
            var a=pool.submit(first);var b=pool.submit(second);start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder("CREATED","COMPANY_CONFLICT");
            assertThat(count("companies")).isEqualTo(1);assertThat(count("outbox")).isEqualTo(1);
        }
    }
    private String createConcurrent(CountDownLatch start,String key) throws Exception {
        start.await();
        try {
            service.create(new Actor(ISSUER,"alice",true),new CreateCompanyCommand("Ejemplo","1","0",key));
            return "CREATED";
        } catch(com.worksoft.company.domain.exception.BusinessException ex) {return ex.code().name();}
    }
    @Test void concurrentIdenticalRequestsCreateOneCompany() throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<UUID> task=()->{start.await();return service.create(new Actor(ISSUER,"alice",true),
                new CreateCompanyCommand("Ejemplo","1","0","creation-key-0001")).id().value();};
            var a=pool.submit(task);var b=pool.submit(task);start.countDown();
            assertThat(a.get(15,TimeUnit.SECONDS)).isEqualTo(b.get(15,TimeUnit.SECONDS));
            assertThat(count("companies")).isEqualTo(1);assertThat(count("outbox")).isEqualTo(1);
        }
    }
}
