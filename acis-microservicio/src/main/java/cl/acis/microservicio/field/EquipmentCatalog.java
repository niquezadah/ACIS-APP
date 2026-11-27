package cl.acis.microservicio.field;

import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/field/equipment")
public class EquipmentCatalog {
    public record Equipment(UUID id,UUID site,String name,String location,boolean active,long lastService) {}
    private final Identity identity;
    private final FieldCatalog catalog;
    private final JdbcTemplate db;
    EquipmentCatalog(Identity identity,FieldCatalog catalog,JdbcTemplate db) {this.identity=identity;this.catalog=catalog;this.db=db;}
    @GetMapping public List<Equipment> list(@RequestHeader("Authorization") String h,@RequestParam UUID site){
        if(!identity.require(h).manager()) throw new ResponseStatusException(FORBIDDEN);
        return db.query("""
            select e.*, (select max(o.departure) from ordenes o join assignments a on a.order_id=o.id
            where a.site=e.site and o.equipment=e.name and o.stage='CLOSED') last_service
            from equipment e where e.site=? order by e.name
            """,(r,n)->new Equipment(r.getObject("id",UUID.class),r.getObject("site",UUID.class),r.getString("name"),r.getString("location"),r.getBoolean("active"),r.getLong("last_service")),site);
    }
    @PutMapping("/{id}") public void save(@RequestHeader("Authorization") String h,@PathVariable UUID id,@RequestBody Equipment e){
        if(!identity.require(h).manager()) throw new ResponseStatusException(FORBIDDEN);
        catalog.active(e.site(),"sites");
        if(e.name()==null || e.name().isBlank() || e.name().length()>140 || e.location()==null || e.location().isBlank() || e.location().length()>180) throw new ResponseStatusException(BAD_REQUEST);
        try{
            if(db.update("update equipment set location=?,active=? where id=? and site=? and name=?",e.location().trim(),e.active(),id,e.site(),e.name().trim())==0)
                db.update("insert into equipment values(?,?,?,?,?)",id,e.site(),e.name().trim(),e.location().trim(),e.active());
        } catch(DuplicateKeyException x){throw new ResponseStatusException(CONFLICT);}
    }
}
