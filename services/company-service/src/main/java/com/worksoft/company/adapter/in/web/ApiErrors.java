package com.worksoft.company.adapter.in.web;

import com.worksoft.company.domain.exception.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
  public static ProblemDetail problem(int status, String code, String title) {
    var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), title);
    p.setType(
        java.net.URI.create("urn:worksoft:problem:" + code.toLowerCase(java.util.Locale.ROOT)));
    p.setTitle(title);
    p.setProperty("code", code);
    return p;
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ProblemDetail> business(BusinessException ex) {
    int status =
        switch (ex.code()) {
          case VALIDATION_ERROR, INVALID_JOURNAL_LINE -> 400;
          case NOMINA_NO_ENCONTRADA -> 404;
          case NOMINA_REFERENCIA_INVALIDA,
              NOMINA_CASO_NO_SOPORTADO,
              NOMINA_PARAMETROS_INVALIDOS,
              NOMINA_DEDUCCIONES_INVALIDAS,
              NOMINA_PERIODO_CERRADO,
              NOMINA_RECALCULAR,
              NOMINA_REGISTRO_DUPLICADO,
              NOMINA_SUPERPOSICION ->
              409;
          case FORBIDDEN -> 403;
          case COMPANY_NOT_FOUND,
              ACCOUNT_NOT_FOUND,
              PARTY_NOT_FOUND,
              VOUCHER_NOT_FOUND,
              FACTURACION_NO_ENCONTRADA ->
              404;
          case COMPANY_CONFLICT,
              IDEMPOTENCY_CONFLICT,
              ACCOUNT_CONFLICT,
              ACCOUNT_VERSION_CONFLICT,
              ACCOUNT_HIERARCHY_CONFLICT,
              PARTY_CONFLICT,
              VERSION_CONFLICT,
              VOUCHER_POSTED,
              POSTING_REFERENCE_INVALID ->
              409;
          case PRODUCTO_DUPLICADO,
              CONFIGURACION_FACTURACION_INVALIDA,
              REFERENCIA_FACTURACION_INVALIDA,
              DOCUMENTO_NO_EDITABLE,
              REVERSO_FACTURACION_INVALIDO,
              PAGO_FACTURACION_INVALIDO ->
              409;
          case VOUCHER_UNBALANCED -> 422;
          case TEMPORARILY_UNAVAILABLE -> 503;
        };
    String title =
        switch (ex.code()) {
          case VALIDATION_ERROR -> "Solicitud inválida";
          case NOMINA_NO_ENCONTRADA -> "Registro de nómina no encontrado";
          case NOMINA_REFERENCIA_INVALIDA -> "Revisa el tercero, empleado y contrato de la empresa";
          case NOMINA_CASO_NO_SOPORTADO ->
              "Se requiere contrato ordinario de jornada completa que cubra todo el mes y salario"
                  + " no inferior al mínimo de la vigencia";
          case NOMINA_PARAMETROS_INVALIDOS ->
              "Registra parámetros revisados que cubran el mes completo";
          case NOMINA_DEDUCCIONES_INVALIDAS -> "Las deducciones superan el devengado";
          case NOMINA_PERIODO_CERRADO -> "El estado del periodo no permite esta operación";
          case NOMINA_RECALCULAR -> "Recalcula la nómina con los datos actuales antes de aprobar";
          case NOMINA_REGISTRO_DUPLICADO ->
              "Ya existe ese empleado, código o periodo mensual sin anular";
          case NOMINA_SUPERPOSICION -> "Las fechas se superponen con otro contrato o vigencia";
          case FACTURACION_NO_ENCONTRADA -> "Registro de facturación no encontrado";
          case PRODUCTO_DUPLICADO -> "Ya existe un producto con ese código";
          case CONFIGURACION_FACTURACION_INVALIDA ->
              "Revisa los prefijos reservados y las cuatro cuentas contables de facturación";
          case REFERENCIA_FACTURACION_INVALIDA ->
              "Revisa el cliente, productos y documento de origen de esta empresa";
          case DOCUMENTO_NO_EDITABLE -> "Solo los borradores admiten modificaciones";
          case REVERSO_FACTURACION_INVALIDO ->
              "La reversión total requiere un documento confirmado sin pagos activos";
          case PAGO_FACTURACION_INVALIDO ->
              "Revisa el estado, fecha y saldo disponible del documento";
          case FORBIDDEN -> "Operación no permitida";
          case COMPANY_NOT_FOUND -> "Empresa no encontrada";
          case PARTY_NOT_FOUND -> "Tercero no encontrado";
          case PARTY_CONFLICT -> "La identificación ya está registrada en esta empresa";
          case VERSION_CONFLICT -> "El registro cambió. Actualiza antes de editar";
          case VOUCHER_NOT_FOUND -> "Comprobante no encontrado";
          case VOUCHER_POSTED -> "El comprobante contabilizado no admite modificaciones";
          case VOUCHER_UNBALANCED ->
              "Se requieren al menos dos líneas, débitos y créditos iguales y un total positivo";
          case INVALID_JOURNAL_LINE ->
              "Cada línea requiere cuenta, tercero y un solo valor positivo con máximo dos"
                  + " decimales";
          case POSTING_REFERENCE_INVALID ->
              "Revisa las cuentas y terceros: deben pertenecer a la empresa, estar activos y las"
                  + " cuentas permitir movimientos sin tener hijas";
          case ACCOUNT_NOT_FOUND -> "Cuenta no encontrada";
          case ACCOUNT_CONFLICT -> "El código ya existe en esta empresa";
          case ACCOUNT_VERSION_CONFLICT -> "La cuenta cambió. Actualiza la tabla antes de editar";
          case ACCOUNT_HIERARCHY_CONFLICT ->
              "Revisa la cuenta superior, su estado y sus movimientos";
          case COMPANY_CONFLICT -> "No se puede registrar la empresa";
          case IDEMPOTENCY_CONFLICT -> "La clave ya se utilizó con otros datos";
          case TEMPORARILY_UNAVAILABLE -> "Servicio temporalmente no disponible";
        };
    var builder = ResponseEntity.status(status);
    if (status == 503) builder.header("Retry-After", "1");
    return builder.body(problem(status, ex.code().name(), title));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MissingRequestHeaderException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  ResponseEntity<ProblemDetail> invalid(Exception ex) {
    return ResponseEntity.badRequest().body(problem(400, "VALIDATION_ERROR", "Solicitud inválida"));
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<ProblemDetail> unavailable(DataAccessException ex) {
    return ResponseEntity.status(503)
        .header("Retry-After", "1")
        .body(problem(503, "TEMPORARILY_UNAVAILABLE", "Servicio temporalmente no disponible"));
  }
}
