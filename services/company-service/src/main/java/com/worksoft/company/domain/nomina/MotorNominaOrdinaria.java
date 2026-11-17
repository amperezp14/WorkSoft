package com.worksoft.company.domain.nomina;

import static com.worksoft.company.domain.exception.BusinessException.Code.*;
import static com.worksoft.company.domain.nomina.ReglasNomina.*;

import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.domain.parties.ThirdParty;
import java.math.*;
import java.time.YearMonth;
import java.util.List;

public final class MotorNominaOrdinaria {
  public LiquidacionNomina calcular(
      YearMonth mes,
      ParametrosNomina parametros,
      EmpleadoNomina empleado,
      ThirdParty tercero,
      ContratoNomina contrato,
      int diasAuxilio,
      List<NovedadNomina> novedades) {
    parametros.exigirAplicable(mes);
    contrato.exigirMesCompleto(mes);
    if (tercero.documentType() == ThirdParty.DocumentType.NIT
        || !empleado.activo()
        || !tercero.active()
        || !tercero.id().equals(empleado.terceroId())
        || !contrato.empleadoId().equals(empleado.id()))
      throw new BusinessException(NOMINA_REFERENCIA_INVALIDA);
    exigir(
        diasAuxilio >= 0
            && diasAuxilio <= 30
            && (contrato.auxilioTransporte() || diasAuxilio == 0));
    var salario = importe(contrato.salarioMensual(), true);
    var minimo = importe(parametros.salarioMinimo(), true);
    if (salario.compareTo(minimo) < 0) throw new BusinessException(NOMINA_CASO_NO_SOPORTADO);
    BigDecimal adicional = BigDecimal.ZERO, otras = BigDecimal.ZERO, retencion = BigDecimal.ZERO;
    for (var novedad : novedades) {
      exigir(novedad.empleadoId().equals(empleado.id()));
      var valor = importe(novedad.importe(), true);
      if (novedad.salarial()) adicional = adicional.add(valor);
      else if (novedad.tipo() == NovedadNomina.Tipo.RETENCION_FUENTE)
        retencion = retencion.add(valor);
      else otras = otras.add(valor);
    }
    var salarial = salario.add(adicional);
    var ibc = salarial.min(minimo.multiply(new BigDecimal("25")));
    var auxilio =
        contrato.auxilioTransporte()
                && salarial.compareTo(minimo.multiply(new BigDecimal("2"))) <= 0
            ? importe(parametros.auxilioTransporte(), false)
                .multiply(BigDecimal.valueOf(diasAuxilio))
                .divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP)
            : new BigDecimal("0.00");
    var salud = porcentaje(ibc, parametros.saludTrabajador());
    var pension = porcentaje(ibc, parametros.pensionTrabajador());
    var tasa = tasaSolidaridad(ibc, minimo);
    var solidaridad = porcentaje(ibc, tasa);
    var devengado = salarial.add(auxilio);
    var deducciones = salud.add(pension).add(solidaridad).add(otras).add(retencion);
    var neto = devengado.subtract(deducciones);
    if (neto.signum() < 0) throw new BusinessException(NOMINA_DEDUCCIONES_INVALIDAS);
    return new LiquidacionNomina(
        empleado.id(),
        empleado.codigo(),
        tercero.name(),
        tercero.documentType().name(),
        tercero.documentNumber(),
        empleado.cargo(),
        empleado.version(),
        tercero.version(),
        contrato,
        diasAuxilio,
        novedades,
        dinero(salario),
        dinero(adicional),
        dinero(auxilio),
        dinero(devengado),
        dinero(ibc),
        dinero(salud),
        dinero(pension),
        tasa,
        dinero(solidaridad),
        dinero(otras),
        dinero(retencion),
        dinero(deducciones),
        dinero(neto));
  }

  private String tasaSolidaridad(BigDecimal ibc, BigDecimal minimo) {
    if (ibc.compareTo(minimo.multiply(new BigDecimal("4"))) < 0) return "0.00";
    if (ibc.compareTo(minimo.multiply(new BigDecimal("20"))) > 0) return "2.00";
    if (ibc.compareTo(minimo.multiply(new BigDecimal("19"))) > 0) return "1.80";
    if (ibc.compareTo(minimo.multiply(new BigDecimal("18"))) > 0) return "1.60";
    if (ibc.compareTo(minimo.multiply(new BigDecimal("17"))) > 0) return "1.40";
    if (ibc.compareTo(minimo.multiply(new BigDecimal("16"))) >= 0) return "1.20";
    return "1.00";
  }
}
