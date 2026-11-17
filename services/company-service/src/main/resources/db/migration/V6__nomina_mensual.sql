CREATE TABLE nomina_empleados (
 id uuid PRIMARY KEY,empresa_id uuid NOT NULL REFERENCES companies(id),tercero_id uuid NOT NULL,
 codigo varchar(30) NOT NULL,nombre varchar(300) NOT NULL,contenido jsonb NOT NULL,
 UNIQUE(empresa_id,id),UNIQUE(empresa_id,tercero_id),UNIQUE(empresa_id,codigo),
 FOREIGN KEY(empresa_id,tercero_id) REFERENCES third_parties(company_id,id),
 CHECK(contenido->>'id'=id::text AND contenido->>'empresaId'=empresa_id::text AND contenido->>'terceroId'=tercero_id::text)
);
CREATE TABLE nomina_contratos (
 id uuid PRIMARY KEY,empresa_id uuid NOT NULL,empleado_id uuid NOT NULL,
 inicio date NOT NULL,fin date,nombre varchar(200) NOT NULL,contenido jsonb NOT NULL,
 UNIQUE(empresa_id,id),FOREIGN KEY(empresa_id,empleado_id) REFERENCES nomina_empleados(empresa_id,id),
 CHECK(fin IS NULL OR fin>=inicio),
 CHECK(contenido->>'id'=id::text AND contenido->>'empresaId'=empresa_id::text AND contenido->>'empleadoId'=empleado_id::text)
);
CREATE INDEX nomina_contratos_fechas ON nomina_contratos(empresa_id,empleado_id,inicio,fin);
CREATE TABLE nomina_parametros (
 id uuid PRIMARY KEY,empresa_id uuid NOT NULL REFERENCES companies(id),desde date NOT NULL,hasta date NOT NULL,
 contenido jsonb NOT NULL,CHECK(hasta>=desde),
 CHECK(contenido->>'id'=id::text AND contenido->>'empresaId'=empresa_id::text)
);
CREATE TABLE nomina_periodos (
 id uuid PRIMARY KEY,empresa_id uuid NOT NULL REFERENCES companies(id),anio integer NOT NULL CHECK(anio BETWEEN 1900 AND 9999),
 mes integer NOT NULL CHECK(mes BETWEEN 1 AND 12),estado varchar(12) NOT NULL CHECK(estado IN ('BORRADOR','CALCULADA','APROBADA','PAGADA','ANULADA')),
 nombre varchar(220) NOT NULL,contenido jsonb NOT NULL,UNIQUE(empresa_id,id),
 CHECK(contenido->>'id'=id::text AND contenido->>'empresaId'=empresa_id::text AND contenido->>'estado'=estado AND (contenido->>'anio')::integer=anio AND (contenido->>'mes')::integer=mes)
);
CREATE UNIQUE INDEX nomina_un_periodo_mensual ON nomina_periodos(empresa_id,anio,mes) WHERE estado<>'ANULADA';
CREATE TABLE nomina_periodo_contratos (
 empresa_id uuid NOT NULL,periodo_id uuid NOT NULL,contrato_id uuid NOT NULL,PRIMARY KEY(periodo_id,contrato_id),
 FOREIGN KEY(empresa_id,periodo_id) REFERENCES nomina_periodos(empresa_id,id),
 FOREIGN KEY(empresa_id,contrato_id) REFERENCES nomina_contratos(empresa_id,id)
);
CREATE FUNCTION proteger_nomina_cerrada() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Anule la nómina, no la elimine' USING ERRCODE='23514'; END IF;
 IF OLD.estado IN ('PAGADA','ANULADA') THEN RAISE EXCEPTION 'Nómina cerrada inmutable' USING ERRCODE='23514'; END IF;
 IF OLD.estado='APROBADA' THEN
  IF NEW.estado NOT IN ('PAGADA','ANULADA') OR
   (OLD.contenido-ARRAY['estado','version','actualizadoEn','motivoAnulacion','fechaPago','referenciaPago']) IS DISTINCT FROM
   (NEW.contenido-ARRAY['estado','version','actualizadoEn','motivoAnulacion','fechaPago','referenciaPago']) THEN
   RAISE EXCEPTION 'Liquidación aprobada inmutable' USING ERRCODE='23514';
  END IF;
 END IF;
 IF (to_jsonb(OLD)-ARRAY['estado','contenido']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['estado','contenido']) THEN
  RAISE EXCEPTION 'Identidad del periodo inmutable' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER proteger_nomina_cerrada BEFORE UPDATE OR DELETE ON nomina_periodos
FOR EACH ROW EXECUTE FUNCTION proteger_nomina_cerrada();
