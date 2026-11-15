package com.worksoft.company.adapter.out.persistence;


import java.sql.*;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DuplicateKeyException;
import com.worksoft.company.domain.accounting.Account;
import com.worksoft.company.domain.exception.BusinessException;
import com.worksoft.company.application.port.in.AccountPage;
import com.worksoft.company.application.port.out.AccountStore;
import static com.worksoft.company.domain.exception.BusinessException.Code.*;
public final class PostgresAccountStore implements AccountStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public PostgresAccountStore(JdbcTemplate jdbc,TransactionTemplate transactions){this.jdbc=jdbc;this.transactions=transactions;}
    private Account map(ResultSet rs,int row) throws SQLException {
        return new Account(rs.getObject("id",UUID.class),rs.getObject("company_id",UUID.class),rs.getString("code"),rs.getString("name"),
            Account.Nature.valueOf(rs.getString("nature")),rs.getBoolean("active"),rs.getBoolean("allows_posting"),rs.getString("notes"),rs.getLong("version"),rs.getTimestamp("updated_at").toInstant());
    }
    @Override public AccountPage list(UUID company,String q,int page,int size) {
        String pattern="%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
        String where=" WHERE company_id=? AND (code ILIKE ? OR name ILIKE ?)";
        long count=jdbc.queryForObject("SELECT count(*) FROM chart_accounts"+where,Long.class,company,pattern,pattern);
        var rows=jdbc.query("SELECT * FROM chart_accounts"+where+" ORDER BY code LIMIT ? OFFSET ?",this::map,company,pattern,pattern,size,page*size);
        return new AccountPage(rows,count,page,size);
    }
    private void lockCompany(UUID company) {
        jdbc.execute("SET LOCAL lock_timeout='5s'");
        if(jdbc.query("SELECT id FROM companies WHERE id=? FOR UPDATE",(rs,n)->rs.getObject(1,UUID.class),company).isEmpty()) throw new BusinessException(COMPANY_NOT_FOUND);
    }
    private void hierarchy(Account value) {
        if(value.parentCode()!=null) {
            var parents=jdbc.query("SELECT * FROM chart_accounts WHERE company_id=? AND code=?",this::map,value.companyId(),value.parentCode());
            if(parents.isEmpty() || parents.getFirst().allowsPosting() || value.active() && !parents.getFirst().active()) throw new BusinessException(ACCOUNT_HIERARCHY_CONFLICT);
        }
        if(value.allowsPosting() && jdbc.queryForObject("SELECT count(*) FROM chart_accounts WHERE company_id=? AND parent_code=?",Long.class,value.companyId(),value.code())>0)
            throw new BusinessException(ACCOUNT_HIERARCHY_CONFLICT);
        if(!value.active() && jdbc.queryForObject("SELECT count(*) FROM chart_accounts WHERE company_id=? AND parent_code=? AND active",Long.class,value.companyId(),value.code())>0)
            throw new BusinessException(ACCOUNT_HIERARCHY_CONFLICT);
    }
    @Override public Account create(Account value) {
        try { return transactions.execute(status->{
            lockCompany(value.companyId());hierarchy(value);
            jdbc.update("""
                INSERT INTO chart_accounts(id,company_id,code,name,parent_code,nature,active,allows_posting,notes,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """,value.id(),value.companyId(),value.code(),value.name(),value.parentCode(),value.nature().name(),value.active(),value.allowsPosting(),value.notes(),Timestamp.from(value.updatedAt()));
            return find(value.companyId(),value.id());
        }); } catch(DuplicateKeyException ex){throw new BusinessException(ACCOUNT_CONFLICT);}
    }
    @Override public Account get(UUID company,UUID id) { return find(company,id); }
    private Account find(UUID company,UUID id) {
        return jdbc.query("SELECT * FROM chart_accounts WHERE company_id=? AND id=?",this::map,company,id).stream().findFirst().orElseThrow(()->new BusinessException(ACCOUNT_NOT_FOUND));
    }
    @Override public Account update(UUID id,Account value) {
        return transactions.execute(status->{
            lockCompany(value.companyId());var prior=find(value.companyId(),id);
            if(prior.version()!=value.version())throw new BusinessException(ACCOUNT_VERSION_CONFLICT);
            if(!prior.code().equals(value.code()))throw new BusinessException(VALIDATION_ERROR);
            hierarchy(value);
            int updated=jdbc.update("""
                UPDATE chart_accounts SET name=?,nature=?,active=?,allows_posting=?,notes=?,updated_at=?,version=version+1
                WHERE company_id=? AND id=? AND version=?
                """,value.name(),value.nature().name(),value.active(),value.allowsPosting(),value.notes(),Timestamp.from(value.updatedAt()),value.companyId(),id,value.version());
            if(updated!=1)throw new BusinessException(ACCOUNT_VERSION_CONFLICT);
            return find(value.companyId(),id);
        });
    }
}
