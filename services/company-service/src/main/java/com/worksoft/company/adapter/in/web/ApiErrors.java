package com.worksoft.company.adapter.in.web;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;
import com.worksoft.company.domain.exception.BusinessException;

@RestControllerAdvice
public class ApiErrors {
    public static ProblemDetail problem(int status, String code, String title) {
        var p=ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status),title);p.setType(java.net.URI.create("urn:worksoft:problem:"+code.toLowerCase(java.util.Locale.ROOT)));p.setTitle(title);p.setProperty("code",code);return p;
    }
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> business(BusinessException ex) {
        int status=switch(ex.code()) {
            case VALIDATION_ERROR -> 400; case FORBIDDEN -> 403; case COMPANY_NOT_FOUND -> 404;
            case COMPANY_CONFLICT, IDEMPOTENCY_CONFLICT -> 409; case TEMPORARILY_UNAVAILABLE -> 503;
        };
        String title=switch(ex.code()) {
            case VALIDATION_ERROR -> "Solicitud inválida"; case FORBIDDEN -> "Operación no permitida";
            case COMPANY_NOT_FOUND -> "Empresa no encontrada";
            case COMPANY_CONFLICT -> "No se puede registrar la empresa";
            case IDEMPOTENCY_CONFLICT -> "La clave ya se utilizó con otros datos";
            case TEMPORARILY_UNAVAILABLE -> "Servicio temporalmente no disponible";
        };
        var builder=ResponseEntity.status(status);
        if(status==503)builder.header("Retry-After","1");
        return builder.body(problem(status,ex.code().name(),title));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> invalid(Exception ex) {
        return ResponseEntity.badRequest().body(problem(400,"VALIDATION_ERROR","Solicitud inválida"));
    }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> unavailable(DataAccessException ex) {
        return ResponseEntity.status(503).header("Retry-After","1")
                .body(problem(503,"TEMPORARILY_UNAVAILABLE","Servicio temporalmente no disponible"));
    }
}
