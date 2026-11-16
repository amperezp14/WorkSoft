package com.worksoft.company;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.worksoft.company.application.port.in.Actor;
import com.worksoft.company.application.port.in.JournalUseCases;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres")
@Testcontainers
class JournalsIntegrationTests {
  @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17.6");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry p) {
    p.add("spring.datasource.url", PG::getJdbcUrl);
    p.add("spring.datasource.username", PG::getUsername);
    p.add("spring.datasource.password", PG::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JournalUseCases journals;
  String company, party, debit, credit;

  @BeforeEach
  void setup() throws Exception {
    jdbc.execute("TRUNCATE companies CASCADE");
    company = createCompany("000301");
    party = createParty(company, "000123");
    debit = createAccount("11050501");
    credit = createAccount("11100501");
  }

  private String createCompany(String nit) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/companies")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType("application/json")
                    .content(
                        "{\"legalName\":\"Empresa de prueba\",\"nit\":\""
                            + nit
                            + "\",\"verificationDigit\":\"0\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return id(response);
  }

  private String id(MvcResult response) throws Exception {
    return JsonPath.read(response.getResponse().getContentAsString(), "$.id");
  }

  private String parties(String c) {
    return "/api/v1/companies/" + c + "/third-parties";
  }

  private String base() {
    return "/api/v1/companies/" + company + "/journals";
  }

  private String partyBody(String number, String name, long version) {
    return "{\"documentType\":\"NIT\",\"documentNumber\":\""
        + number
        + "\",\"verificationDigit\":\"0\",\"name\":\""
        + name
        + "\",\"category\":\"OTHER\",\"email\":\"\",\"phone\":\"\",\"address\":\"\",\"active\":true,\"version\":"
        + version
        + "}";
  }

  private String createParty(String c, String number) throws Exception {
    return id(
        mvc.perform(
                post(parties(c))
                    .contentType("application/json")
                    .content(partyBody(number, "Tercero sintético", 0)))
            .andExpect(status().isCreated())
            .andReturn());
  }

  private String createAccount(String code) throws Exception {
    return id(
        mvc.perform(
                post("/api/v1/companies/" + company + "/accounts")
                    .contentType("application/json")
                    .content(
                        "{\"code\":\""
                            + code
                            + "\",\"name\":\"Cuenta de"
                            + " prueba\",\"nature\":\"DEBIT\",\"active\":true,\"allowsPosting\":true,\"notes\":\"\",\"version\":0}"))
            .andExpect(status().isCreated())
            .andReturn());
  }

  private String line(String account, String p, String d, String c) {
    return "{\"accountId\":\""
        + account
        + "\",\"partyId\":\""
        + p
        + "\",\"description\":\"Detalle\",\"debit\":\""
        + d
        + "\",\"credit\":\""
        + c
        + "\"}";
  }

  private String body(String lines, long version) {
    return "{\"header\":{\"date\":\"2026-11-16\",\"type\":\"GENERAL\",\"origin\":\"Operación de"
               + " prueba\",\"description\":\"Comprobante"
               + " sintético\",\"supportReference\":\"Archivo externo"
               + " TEST-1\",\"preparedBy\":\"Responsable de prueba\"},\"lines\":["
        + lines
        + "],\"version\":"
        + version
        + "}";
  }

  private String balanced() {
    return body(
        line(debit, party, "0.10", "0")
            + ","
            + line(debit, party, "0.20", "0")
            + ","
            + line(credit, party, "0", "0.30"),
        0);
  }

  private MvcResult create(String content, String key) throws Exception {
    return mvc.perform(
            post(base())
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(content))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private String draft() throws Exception {
    return id(create(balanced(), UUID.randomUUID().toString()));
  }

  @Test
  void createsEditsAndScopesThirdParties() throws Exception {
    mvc.perform(get(parties(company)))
        .andExpect(jsonPath("$.items[0].documentNumber").value("000123"));
    mvc.perform(
            put(parties(company) + "/" + party)
                .contentType("application/json")
                .content(partyBody("000123", "Nombre actualizado", 0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));
    mvc.perform(
            put(parties(company) + "/" + party)
                .contentType("application/json")
                .content(partyBody("000123", "Edición vieja", 0)))
        .andExpect(status().isConflict());
    mvc.perform(
            post(parties(company))
                .contentType("application/json")
                .content(partyBody("000123", "Duplicado", 0)))
        .andExpect(status().isConflict());
    String other = createCompany("000302");
    createParty(other, "000123");
    mvc.perform(get(parties(other) + "/" + party)).andExpect(status().isNotFound());
  }

  @Test
  void savesUnbalancedDraftAndPostsOnlyAfterBalancing() throws Exception {
    String id = id(create(body(line(debit, party, "10", "0"), 0), UUID.randomUUID().toString()));
    mvc.perform(
            post(base() + "/" + id + "/post")
                .contentType("application/json")
                .content("{\"version\":0}"))
        .andExpect(status().isUnprocessableEntity());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM journal_sequences", Integer.class))
        .isZero();
    mvc.perform(put(base() + "/" + id).contentType("application/json").content(balanced()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));
    mvc.perform(
            post(base() + "/" + id + "/post")
                .contentType("application/json")
                .content("{\"version\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("POSTED"))
        .andExpect(jsonPath("$.displayNumber").value("CC-2026-000001"))
        .andExpect(jsonPath("$.totalDebit").value("0.30"));
  }

  @Test
  void postedIsImmutableAndSnapshotsSurviveMasterChanges() throws Exception {
    String id = draft();
    mvc.perform(
            post(base() + "/" + id + "/post")
                .contentType("application/json")
                .content("{\"version\":0}"))
        .andExpect(status().isOk());
    mvc.perform(put(base() + "/" + id).contentType("application/json").content(balanced()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("VOUCHER_POSTED"));
    mvc.perform(
            put(parties(company) + "/" + party)
                .contentType("application/json")
                .content(partyBody("000123", "Nombre nuevo", 0)))
        .andExpect(status().isOk());
    jdbc.update("UPDATE chart_accounts SET name='Nombre nuevo' WHERE id=?", UUID.fromString(debit));
    mvc.perform(get(base() + "/" + id))
        .andExpect(jsonPath("$.lines[0].partyName").value("Tercero sintético"))
        .andExpect(jsonPath("$.lines[0].accountName").value("Cuenta de prueba"));
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE journal_lines SET debit=2 WHERE voucher_id=? AND line_number=1",
                    UUID.fromString(id)))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE journal_vouchers SET status='DRAFT',number=NULL,posted_at=NULL WHERE"
                        + " id=?",
                    UUID.fromString(id)))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void idempotentCreationAndConcurrentPostingAllocateOnce() throws Exception {
    String key = UUID.randomUUID().toString();
    String id = id(create(balanced(), key));
    assertThat(id(create(balanced(), key))).isEqualTo(id);
    var actor = new Actor("urn:worksoft:local", "workspace", true);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<Long> task =
          () -> {
            start.await();
            return journals.post(actor, UUID.fromString(company), UUID.fromString(id), 0).number();
          };
      var a = pool.submit(task);
      var b = pool.submit(task);
      start.countDown();
      assertThat(a.get(15, TimeUnit.SECONDS)).isEqualTo(b.get(15, TimeUnit.SECONDS));
    }
    assertThat(jdbc.queryForObject("SELECT last_number FROM journal_sequences", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM audit WHERE operation='POST_JOURNAL'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE event_type='com.worksoft.journal.posted.v1'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rejectsForeignReferencesInactivePartiesAndNonPostingAccounts() throws Exception {
    String other = createCompany("000302"), foreign = createParty(other, "000999");
    mvc.perform(
            post(base())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(body(line(debit, foreign, "10", "0"), 0)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("POSTING_REFERENCE_INVALID"));
    String classification =
        jdbc.queryForObject(
            "SELECT id::text FROM chart_accounts WHERE company_id=? AND code='1105'",
            String.class,
            UUID.fromString(company));
    mvc.perform(
            post(base())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(body(line(classification, party, "10", "0"), 0)))
        .andExpect(status().isConflict());
    String id = draft();
    jdbc.update("UPDATE third_parties SET active=false WHERE id=?", UUID.fromString(party));
    mvc.perform(
            post(base() + "/" + id + "/post")
                .contentType("application/json")
                .content("{\"version\":0}"))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM journal_sequences", Integer.class))
        .isZero();
  }

  @Test
  void rejectsAmbiguousMoneyAndProtectsDraftVersion() throws Exception {
    for (String amount : List.of("-1", "1.001", "1e3"))
      mvc.perform(
              post(base())
                  .header("Idempotency-Key", UUID.randomUUID().toString())
                  .contentType("application/json")
                  .content(body(line(debit, party, amount, "0"), 0)))
          .andExpect(status().isBadRequest());
    String id = draft();
    mvc.perform(put(base() + "/" + id).contentType("application/json").content(balanced()))
        .andExpect(status().isOk());
    mvc.perform(put(base() + "/" + id).contentType("application/json").content(balanced()))
        .andExpect(status().isConflict());
    String other = createCompany("000302");
    mvc.perform(get("/api/v1/companies/" + other + "/journals/" + id))
        .andExpect(status().isNotFound());
  }

  @Test
  void rollbackDoesNotConsumeConsecutiveOnOutboxFailure() throws Exception {
    String id = draft();
    jdbc.execute(
        "ALTER TABLE outbox ADD CONSTRAINT fail_journal CHECK"
            + " (event_type<>'com.worksoft.journal.posted.v1')");
    try {
      mvc.perform(
              post(base() + "/" + id + "/post")
                  .contentType("application/json")
                  .content("{\"version\":0}"))
          .andExpect(status().isServiceUnavailable());
      mvc.perform(get(base() + "/" + id)).andExpect(jsonPath("$.status").value("DRAFT"));
      assertThat(jdbc.queryForObject("SELECT count(*) FROM journal_sequences", Integer.class))
          .isZero();
    } finally {
      jdbc.execute("ALTER TABLE outbox DROP CONSTRAINT fail_journal");
    }
    mvc.perform(
            post(base() + "/" + id + "/post")
                .contentType("application/json")
                .content("{\"version\":0}"))
        .andExpect(jsonPath("$.displayNumber").value("CC-2026-000001"));
  }

  @Test
  void assignsDifferentConsecutivesAndResetsByYear() throws Exception {
    String first = draft(), second = draft();
    var actor = new Actor("urn:worksoft:local", "workspace", true);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      var a =
          pool.submit(
              () -> {
                start.await();
                return journals
                    .post(actor, UUID.fromString(company), UUID.fromString(first), 0)
                    .number();
              });
      var b =
          pool.submit(
              () -> {
                start.await();
                return journals
                    .post(actor, UUID.fromString(company), UUID.fromString(second), 0)
                    .number();
              });
      start.countDown();
      assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(1L, 2L);
    }
    String nextYear =
        id(create(balanced().replace("2026-11-16", "2027-01-01"), UUID.randomUUID().toString()));
    mvc.perform(
            post(base() + "/" + nextYear + "/post")
                .contentType("application/json")
                .content("{\"version\":0}"))
        .andExpect(jsonPath("$.displayNumber").value("CC-2027-000001"));
  }

  @Test
  void databaseRefusesPostedHeaderThatDoesNotMatchItsLines() throws Exception {
    String id = id(create(body(line(debit, party, "10", "0"), 0), UUID.randomUUID().toString()));
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE journal_vouchers SET"
                        + " status='POSTED',number=1,posted_at=now(),total_credit=10 WHERE id=?",
                    UUID.fromString(id)))
        .isInstanceOf(DataAccessException.class);
    mvc.perform(get(base() + "/" + id)).andExpect(jsonPath("$.status").value("DRAFT"));
  }
}
