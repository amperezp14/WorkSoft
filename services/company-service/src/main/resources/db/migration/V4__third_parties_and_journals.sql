CREATE TABLE third_parties (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES companies(id),
    document_type varchar(10) NOT NULL CHECK(document_type IN ('NIT','CC','CE','PASSPORT')),
    document_number varchar(30) NOT NULL,
    verification_digit varchar(1),
    name varchar(200) NOT NULL CHECK(length(trim(name))>0),
    category varchar(10) NOT NULL CHECK(category IN ('CUSTOMER','SUPPLIER','BOTH','OTHER')),
    email varchar(200) NOT NULL DEFAULT '',
    phone varchar(30) NOT NULL DEFAULT '',
    address varchar(250) NOT NULL DEFAULT '',
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    updated_at timestamptz NOT NULL,
    UNIQUE(company_id,id),
    UNIQUE(company_id,document_type,document_number),
    CHECK((document_type='NIT' AND document_number ~ '^[0-9]{1,15}$' AND verification_digit ~ '^[0-9]$' AND verification_digit IS NOT NULL)
       OR (document_type<>'NIT' AND document_number ~ '^[A-Z0-9-]{1,30}$' AND verification_digit IS NULL))
);
CREATE INDEX third_parties_company_name ON third_parties(company_id,name,id);
ALTER TABLE chart_accounts ADD CONSTRAINT chart_accounts_company_id_id UNIQUE(company_id,id);
CREATE TABLE journal_sequences (
    company_id uuid NOT NULL REFERENCES companies(id),
    entry_year integer NOT NULL,
    last_number bigint NOT NULL CHECK(last_number>0),
    PRIMARY KEY(company_id,entry_year)
);
CREATE TABLE journal_vouchers (
    id uuid PRIMARY KEY,
    company_id uuid NOT NULL REFERENCES companies(id),
    request_key varchar(128) NOT NULL,
    request_hash varchar(64) NOT NULL,
    company_name varchar(200) NOT NULL,
    company_nit varchar(15) NOT NULL,
    company_dv varchar(1) NOT NULL,
    entry_date date NOT NULL,
    entry_year integer NOT NULL,
    voucher_type varchar(10) NOT NULL CHECK(voucher_type IN ('GENERAL','INGRESO','EGRESO','AJUSTE')),
    origin varchar(200) NOT NULL CHECK(length(trim(origin))>0),
    description varchar(1000) NOT NULL CHECK(length(trim(description))>0),
    support_reference varchar(500) NOT NULL CHECK(length(trim(support_reference))>0),
    prepared_by varchar(100) NOT NULL CHECK(length(trim(prepared_by))>0),
    status varchar(10) NOT NULL CHECK(status IN ('DRAFT','POSTED')),
    number bigint,
    total_debit numeric(22,2) NOT NULL CHECK(total_debit>=0),
    total_credit numeric(22,2) NOT NULL CHECK(total_credit>=0),
    version bigint NOT NULL CHECK(version>=0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    posted_at timestamptz,
    UNIQUE(company_id,id),
    UNIQUE(company_id,entry_year,number),
    UNIQUE(company_id,request_key),
    CHECK(entry_year=extract(year FROM entry_date)),
    CHECK((status='DRAFT' AND number IS NULL AND posted_at IS NULL)
       OR (status='POSTED' AND number>0 AND posted_at IS NOT NULL AND total_debit=total_credit AND total_debit>0))
);
CREATE INDEX journal_vouchers_company_date ON journal_vouchers(company_id,entry_date DESC,id);
CREATE TABLE journal_lines (
    company_id uuid NOT NULL,
    voucher_id uuid NOT NULL,
    line_number integer NOT NULL CHECK(line_number BETWEEN 1 AND 200),
    account_id uuid NOT NULL,
    account_code varchar(10) NOT NULL,
    account_name varchar(200) NOT NULL,
    party_id uuid NOT NULL,
    party_document_type varchar(10) NOT NULL,
    party_document_number varchar(30) NOT NULL,
    party_dv varchar(1),
    party_name varchar(200) NOT NULL,
    description varchar(300) NOT NULL DEFAULT '',
    debit numeric(18,2) NOT NULL CHECK(debit>=0),
    credit numeric(18,2) NOT NULL CHECK(credit>=0),
    PRIMARY KEY(voucher_id,line_number),
    FOREIGN KEY(company_id,voucher_id) REFERENCES journal_vouchers(company_id,id) ON DELETE CASCADE,
    FOREIGN KEY(company_id,account_id) REFERENCES chart_accounts(company_id,id),
    FOREIGN KEY(company_id,party_id) REFERENCES third_parties(company_id,id),
    CHECK((debit>0 AND credit=0) OR (credit>0 AND debit=0))
);
CREATE FUNCTION protect_posted_voucher() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status='POSTED' THEN RAISE EXCEPTION 'Posted vouchers are immutable' USING ERRCODE='23514'; END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER protect_posted_voucher BEFORE UPDATE OR DELETE ON journal_vouchers
FOR EACH ROW EXECUTE FUNCTION protect_posted_voucher();
CREATE FUNCTION protect_posted_lines() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target uuid;
BEGIN
    IF TG_OP<>'INSERT' THEN
        IF EXISTS(SELECT 1 FROM journal_vouchers WHERE id=OLD.voucher_id AND status='POSTED') THEN
            RAISE EXCEPTION 'Posted lines are immutable' USING ERRCODE='23514';
        END IF;
    END IF;
    IF TG_OP<>'DELETE' THEN
        IF EXISTS(SELECT 1 FROM journal_vouchers WHERE id=NEW.voucher_id AND status='POSTED') THEN
            RAISE EXCEPTION 'Posted lines are immutable' USING ERRCODE='23514';
        END IF;
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER protect_posted_lines BEFORE INSERT OR UPDATE OR DELETE ON journal_lines
FOR EACH ROW EXECUTE FUNCTION protect_posted_lines();
CREATE FUNCTION check_posted_balance() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE actual_debit numeric; actual_credit numeric; actual_count integer;
BEGIN
    IF NEW.status='POSTED' THEN
        SELECT count(*),coalesce(sum(debit),0),coalesce(sum(credit),0)
          INTO actual_count,actual_debit,actual_credit FROM journal_lines WHERE voucher_id=NEW.id;
        IF actual_count<2 OR actual_debit<>actual_credit OR actual_debit<>NEW.total_debit OR actual_credit<>NEW.total_credit THEN
            RAISE EXCEPTION 'Posted voucher must match balanced lines' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER check_posted_balance AFTER INSERT OR UPDATE ON journal_vouchers
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_posted_balance();
