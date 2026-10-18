package ujc.projecto.eba.web;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Roteamento do frontend: todas as rotas de página devolvem o mesmo shell (app.html); o router em JS desenha a página certa. */
@Controller
public class SpaController {
    @GetMapping({"/", "/login", "/recuperar",
                 "/{portal:admin|secretaria|professor|diretor|encarregado}",
                 "/{portal:admin|secretaria|professor|diretor|encarregado}/**"})
    public String pagina() { return "forward:/app.html"; }
}
