package ujc.projecto.eba.web;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import ujc.projecto.eba.model.Utilizador;
import ujc.projecto.eba.repo.UtilizadorRepo;
import ujc.projecto.eba.service.RegraException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Sessões por token (em memória). */
@Component class Sessao {
    private final Map<String,Long> tokens = new ConcurrentHashMap<>();
    String criar(Long id) { String t = UUID.randomUUID().toString(); tokens.put(t, id); return t; }
    Long get(String t) { return t == null ? null : tokens.get(t); }
    void fim(String t) { if (t != null) tokens.remove(t); }
}

/** Controlo de acesso por perfil (RD10, RNF09) e registo de ações (RNF08). */
class AuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger("auditoria");
    private final Sessao sessao; private final UtilizadorRepo users;
    AuthInterceptor(Sessao s, UtilizadorRepo u) { sessao = s; users = u; }
    static String token(HttpServletRequest r) { String h = r.getHeader("Authorization"); return h != null && h.startsWith("Bearer ") ? h.substring(7) : null; }
    @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object h) throws Exception {
        Long id = sessao.get(token(req));
        Utilizador u = id == null ? null : users.findById(id).orElse(null);
        if (u == null) { res.sendError(401); return false; }
        String p = req.getRequestURI();
        String need = p.startsWith("/api/encarregado") ? "ENCARREGADO" : p.startsWith("/api/secretaria") ? "SECRETARIA"
                : p.startsWith("/api/professor") ? "PROFESSOR" : p.startsWith("/api/diretor") ? "DIRETOR" : null;
        if (need != null && !need.equals(u.perfil)) { res.sendError(403); return false; }
        if (!"GET".equals(req.getMethod())) log.info("{} ({}) {} {}", u.email, u.perfil, req.getMethod(), p);
        req.setAttribute("u", u);
        return true;
    }
}

@Configuration class WebConfig implements WebMvcConfigurer {
    private final Sessao sessao; private final UtilizadorRepo users;
    WebConfig(Sessao s, UtilizadorRepo u) { sessao = s; users = u; }
    @Override public void addInterceptors(InterceptorRegistry r) {
        r.addInterceptor(new AuthInterceptor(sessao, users)).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login", "/api/auth/recuperar");
    }
}

@RestControllerAdvice class Erros {
    @ExceptionHandler(RegraException.class) ResponseEntity<Map<String,String>> regra(RegraException e) { return ResponseEntity.status(e.status).body(Map.of("erro", e.getMessage())); }
    @ExceptionHandler(Exception.class) ResponseEntity<Map<String,String>> geral(Exception e) { return ResponseEntity.badRequest().body(Map.of("erro", "Pedido inválido: " + e.getMessage())); }
}
