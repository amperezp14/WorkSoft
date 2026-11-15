CREATE TABLE puc_reference_accounts (
    code varchar(10) PRIMARY KEY CHECK (code ~ '^[1-9][0-9]*$' AND length(code) IN (1,2,4,6)),
    name varchar(200) NOT NULL,
    parent_code varchar(10) REFERENCES puc_reference_accounts(code),
    nature varchar(6) NOT NULL CHECK (nature IN ('DEBIT','CREDIT')),
    reference_year integer NOT NULL DEFAULT 2026,
    source_norm varchar(100) NOT NULL DEFAULT 'Decreto 2650 de 1993 - referencia parcial'
);
CREATE TABLE chart_accounts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES companies(id),
    code varchar(10) NOT NULL CHECK (code ~ '^[1-9][0-9]*$' AND length(code) IN (1,2,4,6,8,10)),
    name varchar(200) NOT NULL CHECK (length(trim(name))>0),
    parent_code varchar(10),
    nature varchar(6) NOT NULL CHECK (nature IN ('DEBIT','CREDIT')),
    active boolean NOT NULL DEFAULT true,
    allows_posting boolean NOT NULL DEFAULT false CHECK (NOT allows_posting OR length(code)>=6),
    notes varchar(1000) NOT NULL DEFAULT '',
    reference_code varchar(10) REFERENCES puc_reference_accounts(code),
    version bigint NOT NULL DEFAULT 0 CHECK (version>=0),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(company_id,code),
    FOREIGN KEY(company_id,parent_code) REFERENCES chart_accounts(company_id,code),
    CHECK (parent_code IS NOT DISTINCT FROM CASE WHEN length(code)=1 THEN NULL WHEN length(code)=2 THEN left(code,1) ELSE left(code,length(code)-2) END)
);
CREATE INDEX chart_accounts_company_order ON chart_accounts(company_id,code);
