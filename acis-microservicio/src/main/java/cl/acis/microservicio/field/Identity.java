package cl.acis.microservicio.field;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class Identity {
    public record User(UUID id,String email,String name,String role,String scope) {
        boolean manager() { return "Jefatura".equals(role); }
        boolean admin() { return "Administrador".equals(role); }
        boolean sees(UUID technician, UUID site) {
            return manager() || ("Técnico".equals(role) && technician.toString().equals(scope))
                || ("Cliente".equals(role) && site.toString().equals(scope));
        }
    }
    private final RestClient accounts;
    Identity(@Value("${ACIS_ACCOUNTS_URL:http://127.0.0.1:8081}") String url) { accounts=RestClient.create(url); }
    User require(String auth) {
        try { return accounts.get().uri("/accounts/me").header("Authorization",auth).retrieve().body(User.class); }
        catch(HttpClientErrorException e) { throw new ResponseStatusException(UNAUTHORIZED); }
        catch(RestClientException e) { throw new ResponseStatusException(SERVICE_UNAVAILABLE); }
    }
}
