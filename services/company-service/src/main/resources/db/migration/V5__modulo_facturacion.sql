-- Numeración comercial interna, sin autorización DIAN.
ALTER TABLE audit ADD COLUMN entity_id uuid;
CREATE TABLE facturacion_productos (
 id uuid PRIMARY KEY,empresa_id uuid NOT NULL REFERENCES companies(id),codigo varchar(40) NOT NULL,
 nombre varchar(200) NOT NULL,activo boolean NOT NULL,version bigint NOT NULL CHECK(version>=0),contenido jsonb NOT NULL,
 UNIQUE(empresa_id,id),UNIQUE(empresa_id,codigo),
 CHECK(contenido->>'id'=id::text AND contenido->>'empresaId'=empresa_id::text)
);
CREATE TABLE facturacion_configuracion (empresa_id uuid PRIMARY KEY REFERENCES companies(id),contenido jsonb NOT NULL);
-- Los prefijos utilizados quedan reservados a su tipo para evitar colisiones históricas.
CREATE TABLE facturacion_prefijos (
 empresa_id uuid NOT NULL REFERENCES companies(id),prefijo varchar(8) NOT NULL,tipo varchar(20) NOT NULL,
 PRIMARY KEY(empresa_id,prefijo)
);
CREATE TABLE facturacion_consecutivos (
 empresa_id uuid NOT NULL REFERENCES companies(id),tipo varchar(20) NOT NULL,
 anio integer NOT NULL CHECK(anio BETWEEN 1 AND 9999),ultimo bigint NOT NULL CHECK(ultimo>0),PRIMARY KEY(empresa_id,tipo,anio)
);
CREATE TABLE facturacion_documentos (
 id uuid PRIMARY KEY,empresa_id uuid NOT NULL REFERENCES companies(id),cliente_id uuid NOT NULL,origen_id uuid,
 tipo varchar(20) NOT NULL CHECK(tipo IN ('FACTURA','NOTA_CREDITO','NOTA_DEBITO')),
 estado varchar(20) NOT NULL CHECK(estado IN ('BORRADOR','CONFIRMADO','ANULADO')),numero varchar(40),
 fecha date NOT NULL,vencimiento date NOT NULL CHECK(vencimiento>=fecha),
 total numeric(18,2) NOT NULL CHECK(total>=0),iva numeric(18,2) NOT NULL CHECK(iva>=0 AND iva<=total),
 version bigint NOT NULL CHECK(version>=0),contenido jsonb NOT NULL,
 UNIQUE(empresa_id,id),UNIQUE(empresa_id,numero),
 FOREIGN KEY(empresa_id,cliente_id) REFERENCES third_parties(company_id,id),
 FOREIGN KEY(empresa_id,origen_id) REFERENCES facturacion_documentos(empresa_id,id),
 CHECK((tipo='FACTURA' AND origen_id IS NULL) OR (tipo<>'FACTURA' AND origen_id IS NOT NULL)),
 CHECK((estado='BORRADOR' AND numero IS NULL) OR (estado<>'BORRADOR' AND numero IS NOT NULL AND total>0)),
 CHECK(contenido->>'id'=id::text AND contenido->>'empresaId'=empresa_id::text AND contenido->>'estado'=estado AND (contenido->>'version')::bigint=version AND (contenido->'totales'->>'total')::numeric=total)
);
CREATE UNIQUE INDEX facturacion_un_credito_total ON facturacion_documentos(empresa_id,origen_id) WHERE tipo='NOTA_CREDITO';
CREATE INDEX facturacion_documentos_listado ON facturacion_documentos(empresa_id,fecha DESC,id);
CREATE TABLE facturacion_referencias (
 empresa_id uuid NOT NULL,documento_id uuid NOT NULL,producto_id uuid NOT NULL,PRIMARY KEY(documento_id,producto_id),
 FOREIGN KEY(empresa_id,documento_id) REFERENCES facturacion_documentos(empresa_id,id),
 FOREIGN KEY(empresa_id,producto_id) REFERENCES facturacion_productos(empresa_id,id)
);
CREATE TABLE facturacion_solicitudes (
 empresa_id uuid NOT NULL,clave varchar(128) NOT NULL,huella varchar(64) NOT NULL,documento_id uuid NOT NULL,
 PRIMARY KEY(empresa_id,clave),FOREIGN KEY(empresa_id,documento_id) REFERENCES facturacion_documentos(empresa_id,id)
);
CREATE TABLE facturacion_pagos (
 id uuid PRIMARY KEY,empresa_id uuid NOT NULL,documento_id uuid NOT NULL,fecha date NOT NULL,
 importe numeric(18,2) NOT NULL CHECK(importe>0),anulado boolean NOT NULL,clave varchar(128) NOT NULL,
 huella varchar(64) NOT NULL,creado_en timestamptz NOT NULL,contenido jsonb NOT NULL,
 UNIQUE(empresa_id,clave),FOREIGN KEY(empresa_id,documento_id) REFERENCES facturacion_documentos(empresa_id,id),
 CHECK(contenido->>'id'=id::text AND contenido->>'empresaId'=empresa_id::text AND (contenido->>'anulado')::boolean=anulado AND (contenido->>'importe')::numeric=importe)
);
CREATE INDEX facturacion_pagos_documento ON facturacion_pagos(empresa_id,documento_id);
CREATE FUNCTION proteger_documento_comercial() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.estado<>'BORRADOR' THEN
  IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Documento comercial inmutable' USING ERRCODE='23514'; END IF;
  IF OLD.estado='ANULADO' OR NEW.estado<>'ANULADO' OR
   (OLD.contenido - ARRAY['estado','version','actualizadoEn']) IS DISTINCT FROM
   (NEW.contenido - ARRAY['estado','version','actualizadoEn']) OR
   (to_jsonb(OLD) - ARRAY['contenido','estado','version']) IS DISTINCT FROM
   (to_jsonb(NEW) - ARRAY['contenido','estado','version']) THEN
   RAISE EXCEPTION 'Documento comercial inmutable' USING ERRCODE='23514';
  END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER proteger_documento_comercial BEFORE UPDATE OR DELETE ON facturacion_documentos
FOR EACH ROW EXECUTE FUNCTION proteger_documento_comercial();
CREATE FUNCTION proteger_recaudo_comercial() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'El recaudo debe anularse, no eliminarse' USING ERRCODE='23514'; END IF;
 IF OLD.anulado OR NOT NEW.anulado OR
  (to_jsonb(OLD)-ARRAY['anulado','contenido']) IS DISTINCT FROM (to_jsonb(NEW)-ARRAY['anulado','contenido']) OR
  (OLD.contenido-ARRAY['anulado','fechaAnulacion','motivoAnulacion','anuladoPor','comprobanteReversionId']) IS DISTINCT FROM
  (NEW.contenido-ARRAY['anulado','fechaAnulacion','motivoAnulacion','anuladoPor','comprobanteReversionId']) THEN
  RAISE EXCEPTION 'El recaudo solo admite anulación trazable' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER proteger_recaudo_comercial BEFORE UPDATE OR DELETE ON facturacion_pagos
FOR EACH ROW EXECUTE FUNCTION proteger_recaudo_comercial();
