package cl.acis.cuentas;

import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
@RestController
@RequestMapping("/accounts")
public class Accounts {
    public record Profile(UUID id,String email,String name,String role,String scope) {}
    public record Credentials(String email,String password,String name,String recovery) {}
    public record Access(String role,String scope) {}
    public record Registration(String recovery) {}
    public record Session(String token,long expires,Profile profile) {}
    private final JdbcTemplate db;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private final SecureRandom random = new SecureRandom();
    public Accounts(JdbcTemplate db) { this.db=db; }
    private String secret() {
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static String email(String v) {
        if(v==null || v.length()>254 || !v.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new ResponseStatusException(BAD_REQUEST);
        return v.trim().toLowerCase(Locale.ROOT);
    }
    static String name(String v) {
        if(v==null || v.trim().length()<2 || v.trim().length()>120) throw new ResponseStatusException(BAD_REQUEST);
        return v.trim();
    }
    static String password(String v) {
        if(v==null || v.length()<10 || v.getBytes(StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(BAD_REQUEST);
        return v;
    }
    @Bean ApplicationRunner bootstrap() {
        return args -> {
            String address=System.getenv("ACIS_ADMIN_EMAIL"), pass=System.getenv("ACIS_ADMIN_PASSWORD");
            if(address!=null && pass!=null && db.queryForObject("select count(*) from accounts", Integer.class)==0)
                create(new Credentials(address,pass,"Administración",""),"Administrador");
        };
    }
    private Registration create(Credentials c,String role) {
        String recovery=secret();
        try {
            db.update("insert into accounts(id,email,display_name,password_hash,recovery_hash,role) values(?,?,?,?,?,?)",
                UUID.randomUUID(),email(c.email()),name(c.name()),passwords.encode(password(c.password())),digest(recovery),role);
        } catch(DuplicateKeyException e) { throw new ResponseStatusException(CONFLICT); }
        return new Registration(recovery);
    }
    @PostMapping("/register") public Registration register(@RequestBody Credentials c) { return create(c,"Cliente"); }
    private Profile profile(java.sql.ResultSet r,int row) throws java.sql.SQLException {
        return new Profile(r.getObject("id",UUID.class),r.getString("email"),r.getString("display_name"),r.getString("role"),r.getString("scope"));
    }
    @PostMapping("/login") @Transactional public Session login(@RequestBody Credentials c) {
        String address=email(c.email());
        var hashes=db.queryForList("select password_hash from accounts where email=?",String.class,address);
        if(c.password()==null || hashes.isEmpty() || !passwords.matches(c.password(),hashes.getFirst())) throw new ResponseStatusException(UNAUTHORIZED);
        Profile p=db.queryForObject("select * from accounts where email=?",this::profile,address);
        String token=secret(); long expires=System.currentTimeMillis()+Duration.ofDays(7).toMillis();
        db.update("delete from sessions where expires_at<=?",System.currentTimeMillis());
        db.update("insert into sessions values(?,?,?)",digest(token),p.id(),expires);
        return new Session(token,expires,p);
    }
    @GetMapping("/me") public Profile me(@RequestHeader("Authorization") String header) {
        if(!header.startsWith("Bearer ")) throw new ResponseStatusException(UNAUTHORIZED);
        var users=db.query("select a.* from accounts a join sessions s on s.account_id=a.id where s.digest=? and s.expires_at>?",this::profile,digest(header.substring(7)),System.currentTimeMillis());
        if(users.isEmpty()) throw new ResponseStatusException(UNAUTHORIZED);
        return users.getFirst();
    }
    @PatchMapping("/me") public Profile edit(@RequestHeader("Authorization") String h,@RequestBody Credentials c) {
        db.update("update accounts set display_name=? where id=?",name(c.name()),me(h).id()); return me(h);
    }
    @PostMapping("/recover") @Transactional public Registration recover(@RequestBody Credentials c) {
        String address=email(c.email()), next=secret();
        if(c.recovery()==null || db.update("update accounts set recovery_hash=?,password_hash=? where email=? and recovery_hash=?",digest(next),passwords.encode(password(c.password())),address,digest(c.recovery().trim()))!=1) throw new ResponseStatusException(UNAUTHORIZED);
        db.update("delete from sessions where account_id=(select id from accounts where email=?)",address);
        return new Registration(next);
    }
    @PostMapping("/logout") public void logout(@RequestHeader("Authorization") String h) {
        me(h); db.update("delete from sessions where digest=?",digest(h.substring(7)));
    }
    private Profile admin(String h) {
        var p=me(h); if(!p.role().equals("Administrador")) throw new ResponseStatusException(FORBIDDEN); return p;
    }
    @GetMapping public List<Profile> users(@RequestHeader("Authorization") String h) {
        admin(h); return db.query("select * from accounts order by email",this::profile);
    }
    @PutMapping("/{id}/access") @Transactional public void access(@RequestHeader("Authorization") String h,@PathVariable UUID id,@RequestBody Access a) {
        if(admin(h).id().equals(id) || !List.of("Cliente","Técnico","Jefatura","Administrador").contains(a.role())) throw new ResponseStatusException(BAD_REQUEST);
        String scope=a.scope()==null?"":a.scope().trim();
        if(!scope.isEmpty()) { try { UUID.fromString(scope); } catch(IllegalArgumentException e) { throw new ResponseStatusException(BAD_REQUEST); } }
        if(db.update("update accounts set role=?,scope=? where id=?",a.role(),scope,id)!=1) throw new ResponseStatusException(NOT_FOUND);
        db.update("delete from sessions where account_id=?",id);
    }
}
