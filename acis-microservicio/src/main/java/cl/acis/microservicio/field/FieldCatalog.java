package cl.acis.microservicio.field;

import java.sql.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/field/catalog")
public class FieldCatalog {
    record Entry(UUID id,String kind,String name,String address,String client,String email,boolean active) {}
    private final JdbcTemplate db;
    private final Identity identity;
    FieldCatalog(JdbcTemplate db,Identity identity) { this.db=db; this.identity=identity; }
    Entry row(ResultSet r,int index) throws SQLException {
        return new Entry(r.getObject("id",UUID.class),r.getString("kind"),r.getString("name"),r.getString("address"),r.getString("client"),r.getString("email"),r.getBoolean("active"));
    }
    Entry active(UUID id,String kind) {
        var entries=db.query("select * from catalog_entries where id=? and kind=? and active=true",this::row,id,kind);
        if(entries.isEmpty()) throw new ResponseStatusException(BAD_REQUEST);
        return entries.getFirst();
    }
    @GetMapping public List<Entry> list(@RequestHeader("Authorization") String auth) {
        var user=identity.require(auth);
        if(!user.manager() && !user.admin()) return List.of();
        return db.query("select * from catalog_entries order by name",this::row);
    }
    @PutMapping("/{id}") public void save(@RequestHeader("Authorization") String auth,@PathVariable UUID id,@RequestBody Entry e) {
        if(!identity.require(auth).manager()) throw new ResponseStatusException(FORBIDDEN);
        boolean tech="technicians".equals(e.kind());
        if(!tech && !"sites".equals(e.kind()) || e.name()==null || e.name().trim().length()<2) throw new ResponseStatusException(BAD_REQUEST);
        String email=Objects.toString(e.email(),"").trim().toLowerCase(Locale.ROOT),address=Objects.toString(e.address(),"").trim(),client=Objects.toString(e.client(),"").trim();
        if(tech && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || !tech && (address.length()<5 || client.length()<2) || e.name().length()>140 || address.length()>255 || client.length()>140 || email.length()>254) throw new ResponseStatusException(BAD_REQUEST);
        String key=tech?email:(e.name().trim()+"|"+address).toLowerCase(Locale.ROOT);
        try {
            if(db.update("update catalog_entries set name=?,address=?,client=?,email=?,active=?,business_key=? where id=? and kind=?",e.name().trim(),address,client,email,e.active(),key,id,e.kind())==0)
                db.update("insert into catalog_entries(id,kind,name,address,client,email,active,business_key) values(?,?,?,?,?,?,?,?)",id,e.kind(),e.name().trim(),address,client,email,e.active(),key);
        } catch(DuplicateKeyException ex) { throw new ResponseStatusException(CONFLICT); }
    }
}
