CREATE TABLE companies (
    id uuid PRIMARY KEY,
    legal_name varchar(200) NOT NULL CHECK (length(trim(legal_name))>0),
    nit varchar(15) NOT NULL UNIQUE CHECK (nit ~ '^[0-9]{1,15}$'),
    verification_digit varchar(1) NOT NULL CHECK (verification_digit ~ '^[0-9]$'),
    created_at timestamptz NOT NULL
);
CREATE TABLE memberships (
    company_id uuid NOT NULL REFERENCES companies(id), issuer varchar(500) NOT NULL,
    subject varchar(255) NOT NULL, role varchar(20) NOT NULL CHECK (role IN ('ADMIN','READER')),
    active boolean NOT NULL, PRIMARY KEY(company_id,issuer,subject)
);
CREATE INDEX memberships_actor ON memberships(issuer,subject,company_id) WHERE active;
CREATE INDEX companies_page ON companies(created_at,id);
CREATE TABLE idempotency (
    issuer varchar(500) NOT NULL, subject varchar(255) NOT NULL, operation varchar(40) NOT NULL,
    key varchar(128) NOT NULL, request_hash varchar(64) NOT NULL,
    company_id uuid NOT NULL REFERENCES companies(id), expires_at timestamptz NOT NULL,
    PRIMARY KEY(issuer,subject,operation,key)
);
CREATE INDEX idempotency_expiration ON idempotency(expires_at);
CREATE TABLE audit (
    id uuid PRIMARY KEY, company_id uuid NOT NULL REFERENCES companies(id),
    issuer varchar(500) NOT NULL, subject varchar(255) NOT NULL,
    operation varchar(40) NOT NULL, occurred_at timestamptz NOT NULL
);
CREATE TABLE outbox (
    event_id uuid PRIMARY KEY, company_id uuid NOT NULL REFERENCES companies(id),
    event_type varchar(100) NOT NULL, payload jsonb NOT NULL,
    occurred_at timestamptz NOT NULL, published_at timestamptz
);
CREATE INDEX outbox_pending ON outbox(occurred_at,event_id) WHERE published_at IS NULL;
