package cl.acis.microservicio.field;

import cl.acis.microservicio.dto.*;
import cl.acis.microservicio.model.Stage;
import cl.acis.microservicio.repository.WorkOrderRepository;
import cl.acis.microservicio.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/field/orders")
public class FieldOrders {
    public record Plan(@NotNull UUID site,@NotNull UUID technician,String scheduled,Double latitude,Double longitude,int slaHours) {}
    public record Write(@NotNull UUID operation,long revision,@Valid @NotNull Plan plan,@Valid @NotNull OrderRequest order,@NotNull @Size(max=10) List<String> photos) {}
    public record Visit(OrderResponse order,Plan plan,long revision,long created,List<String> photos) {}
    record Assignment(long id,Plan plan,long revision,long created) {}
    private final JdbcTemplate db;
    private final Identity identity;
    private final OrderService service;
    private final WorkOrderRepository orders;
    private final FieldCatalog catalog;
    private final ObjectMapper json;
    FieldOrders(JdbcTemplate db,Identity identity,OrderService service,WorkOrderRepository orders,FieldCatalog catalog,ObjectMapper json) {
        this.db=db; this.identity=identity; this.service=service; this.orders=orders; this.catalog=catalog; this.json=json;
    }
    private Assignment row(java.sql.ResultSet r,int i) throws java.sql.SQLException {
        return new Assignment(r.getLong("order_id"),new Plan(r.getObject("site",UUID.class),r.getObject("technician",UUID.class),r.getString("scheduled"),r.getObject("latitude",Double.class),r.getObject("longitude",Double.class),r.getInt("sla_hours")),r.getLong("revision"),r.getLong("created_at"));
    }
    Assignment access(Identity.User user,long id,boolean lock) {
        var rows=db.query("select * from assignments where order_id=?"+(lock?" for update":""),this::row,id);
        if(rows.isEmpty() || !user.sees(rows.getFirst().plan().technician(),rows.getFirst().plan().site())) throw new ResponseStatusException(NOT_FOUND);
        return rows.getFirst();
    }
    private Visit visit(Assignment a) {
        return new Visit(service.findById(a.id()),a.plan(),a.revision(),a.created(),
            db.queryForList("select digest from order_photos where order_id=? order by position_no",String.class,a.id()));
    }
    @GetMapping @Transactional(readOnly=true) public List<Visit> list(@RequestHeader("Authorization") String h) {
        var user=identity.require(h);
        return db.query("select * from assignments order by order_id desc",this::row).stream()
            .filter(a->user.sees(a.plan().technician(),a.plan().site())).map(this::visit).toList();
    }
    private static void checkPlan(Plan p) {
        try { LocalDate.parse(p.scheduled()); } catch(RuntimeException e) { throw new ResponseStatusException(BAD_REQUEST); }
        if(p.slaHours()<1 || p.slaHours()>8760 || (p.latitude()==null)!=(p.longitude()==null) ||
            p.latitude()!=null && (Math.abs(p.latitude())>90 || Math.abs(p.longitude())>180)) throw new ResponseStatusException(BAD_REQUEST);
    }
    private void select(Plan p,OrderRequest o) {
        checkPlan(p);
        var site=catalog.active(p.site(),"sites"); var tech=catalog.active(p.technician(),"technicians");
        if(db.queryForObject("select count(*) from equipment where site=? and name=? and active=true",Integer.class,p.site(),o.getEquipment().trim())==0) throw new ResponseStatusException(BAD_REQUEST);
        o.setSite(site.name()); o.setAddress(site.address()); o.setTechnician(tech.name());
    }
    private boolean replay(UUID op,Identity.User user,long id) {
        return db.queryForObject("select count(*) from changes where operation=? and account=? and order_id=?",Integer.class,op,user.id(),id)>0;
    }
    private void log(UUID op,Identity.User user,long id,long version,String action) {
        db.update("insert into changes(operation,account,order_id,revision,action,recorded_at,actor_name) values(?,?,?,?,?,?,?)",op,user.id(),id,version,action,System.currentTimeMillis(),user.name());
    }
    @PostMapping @Transactional public Visit create(@RequestHeader("Authorization") String h,@Valid @RequestBody Write w) {
        var user=identity.require(h); if(!user.manager()) throw new ResponseStatusException(FORBIDDEN);
        var prior=db.queryForList("select order_id from changes where operation=? and account=?",Long.class,w.operation(),user.id());
        if(!prior.isEmpty()) return visit(access(user,prior.getFirst(),false));
        var p=w.plan(); var o=w.order(); select(p,o);
        if(w.revision()!=0 || !w.photos().isEmpty() || o.getArrival()!=0 || o.getDeparture()!=0) throw new ResponseStatusException(BAD_REQUEST);
        long id=service.create(o).id();
        db.update("insert into assignments values(?,?,?,?,?,?,?,?,?)",id,p.technician(),p.site(),1,p.scheduled(),p.latitude(),p.longitude(),p.slaHours(),System.currentTimeMillis());
        log(w.operation(),user,id,1,"Creación"); return visit(access(user,id,false));
    }
    private void allowed(String role,Visit old,Write w) {
        var before=json.valueToTree(old.order()); var after=json.valueToTree(w.order());
        var stage=old.order().stage(); var next=w.order().getStage();
        Set<String> fields;
        if("Técnico".equals(role) && (stage==Stage.ASSIGNED || stage==Stage.ACCEPTED || stage==Stage.ACTIVE)) {
            fields=switch(stage) {
                case ASSIGNED -> Set.of("stage");
                case ACCEPTED -> Set.of("stage","arrival");
                default -> Set.of("stage","diagnosis","work","materials","location","departure","evidenceCount");
            };
            if(next==Stage.ACTIVE && w.order().getArrival()<=0 || next==Stage.DONE && w.order().getDeparture()<w.order().getArrival()) throw new ResponseStatusException(BAD_REQUEST);
        } else if("Cliente".equals(role) && stage==Stage.DONE && next==stage && !old.order().customerConfirmed() && w.order().isCustomerConfirmed()) {
            fields=Set.of("customer","feedback","customerConfirmed");
        } else if("Jefatura".equals(role) && stage==Stage.DONE && next==Stage.CLOSED) {
            fields=Set.of("stage");
        } else if("Jefatura".equals(role) && stage==Stage.ASSIGNED && next==stage) {
            fields=Set.of("technician","site","address","priority","type","problem","equipment");
        } else throw new ResponseStatusException(FORBIDDEN);
        for(String key:before.propertyNames()) {
            if(!key.equals("id") && !fields.contains(key) && !Objects.equals(before.get(key),after.get(key))) throw new ResponseStatusException(FORBIDDEN);
        }
        if(!("Jefatura".equals(role) && stage==Stage.ASSIGNED) && !old.plan().equals(w.plan())) throw new ResponseStatusException(FORBIDDEN);
        if(!("Técnico".equals(role) && stage==Stage.ACTIVE) && !old.photos().equals(w.photos())) throw new ResponseStatusException(FORBIDDEN);
    }
    @PutMapping("/{id}") @Transactional public Visit update(@RequestHeader("Authorization") String h,@PathVariable long id,@Valid @RequestBody Write w) {
        var user=identity.require(h); var a=access(user,id,true);
        if(replay(w.operation(),user,id)) return visit(a);
        if(a.revision()!=w.revision()) throw new ResponseStatusException(CONFLICT);
        var old=visit(a); allowed(user.role(),old,w);
        if(w.photos().size()!=w.order().getEvidenceCount()) throw new ResponseStatusException(BAD_REQUEST);
        for(String hash:w.photos()) if(db.queryForObject("select count(*) from photo_files where order_id=? and digest=?",Integer.class,id,hash)==0) throw new ResponseStatusException(BAD_REQUEST);
        var p=w.plan();
        if(user.manager() && old.order().stage()==Stage.ASSIGNED) select(p,w.order());
        service.update(id,w.order());
        db.update("update assignments set technician=?,site=?,scheduled=?,latitude=?,longitude=?,sla_hours=?,revision=revision+1 where order_id=?",p.technician(),p.site(),p.scheduled(),p.latitude(),p.longitude(),p.slaHours(),id);
        db.update("delete from order_photos where order_id=?",id);
        int i=0; for(String hash:w.photos()) db.update("insert into order_photos values(?,?,?)",id,i++,hash);
        log(w.operation(),user,id,a.revision()+1,w.order().getStage().name());
        return visit(access(user,id,false));
    }
    @DeleteMapping("/{id}") @Transactional public void delete(@RequestHeader("Authorization") String h,@PathVariable long id,@RequestParam long revision) {
        var user=identity.require(h); if(!user.manager()) throw new ResponseStatusException(FORBIDDEN);
        if(access(user,id,true).revision()!=revision) throw new ResponseStatusException(CONFLICT);
        db.update("delete from assignments where order_id=?",id); orders.deleteById(id);
        log(UUID.randomUUID(),user,id,revision+1,"Eliminación");
    }
    @PutMapping("/{id}/photos/{hash}") @Transactional public void upload(@RequestHeader("Authorization") String h,@PathVariable long id,@PathVariable String hash,jakarta.servlet.http.HttpServletRequest req) throws Exception {
        var user=identity.require(h); access(user,id,true);
        if(!user.role().equals("Técnico")) throw new ResponseStatusException(FORBIDDEN);
        if(db.queryForObject("select count(*) from photo_files where order_id=? and digest=?",Integer.class,id,hash)>0) return;
        if(service.findById(id).stage()!=Stage.ACTIVE) throw new ResponseStatusException(FORBIDDEN);
        byte[] data=req.getInputStream().readNBytes(16*1024*1024+1);
        if(data.length==0 || data.length>16*1024*1024 || !HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data)).equals(hash)) throw new ResponseStatusException(BAD_REQUEST);
        if(db.queryForObject("select count(*) from photo_files where order_id=? and digest=?",Integer.class,id,hash)==0)
            db.update("insert into photo_files values(?,?,?)",id,hash,data);
    }
    record Event(String name,String action,long at) {}
    @GetMapping("/{id}/history") public List<Event> history(@RequestHeader("Authorization") String h,@PathVariable long id) {
        access(identity.require(h),id,false);
        return db.query("select actor_name,action,recorded_at from changes where order_id=? order by revision",(r,n)->new Event(r.getString(1),r.getString(2),r.getLong(3)),id);
    }
    @GetMapping(value="/{id}/photos/{hash}",produces="application/octet-stream") public byte[] photo(@RequestHeader("Authorization") String h,@PathVariable long id,@PathVariable String hash) {
        access(identity.require(h),id,false);
        var rows=db.queryForList("select f.image from photo_files f join order_photos p on p.order_id=f.order_id and p.digest=f.digest where f.order_id=? and f.digest=?",byte[].class,id,hash);
        if(rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND); return rows.getFirst();
    }
}
