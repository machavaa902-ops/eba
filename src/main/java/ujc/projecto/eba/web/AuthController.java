package ujc.projecto.eba.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import ujc.projecto.eba.service.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static ujc.projecto.eba.service.Util.*;

import org.slf4j.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UtilizadorRepo users; private final Sessao sessao;
    AuthController(UtilizadorRepo u, Sessao s) { users = u; sessao = s; }

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody Map<String,String> b) {
        String email = b.getOrDefault("email", "").trim().toLowerCase();
        Utilizador u = users.findByEmail(email).filter(x -> x.ativo && Seguranca.confere(b.get("senha"), x))
                .orElseThrow(() -> new RegraException(401, "Email ou palavra-passe incorretos."));
        return Map.of("token", sessao.criar(u.id), "perfil", u.perfil, "nome", u.nome);       // RD10: o frontend redireciona pelo perfil
    }
    @PostMapping("/recuperar")
    public Map<String,String> recuperar(@RequestBody Map<String,String> b) {
        users.findByEmail(b.getOrDefault("email", "").trim().toLowerCase())
             .ifPresent(u -> log.info("[SIMULAÇÃO DE EMAIL] link de redefinição enviado para {}", u.email));
        return Map.of("mensagem", "Se o email estiver registado, receberá em instantes uma mensagem com o link, válido por 30 minutos.");
    }
    @PostMapping("/logout") public void logout(@RequestHeader(value = "Authorization", required = false) String h) { if (h != null) sessao.fim(h.replace("Bearer ", "")); }
}
