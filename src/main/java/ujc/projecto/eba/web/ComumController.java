package ujc.projecto.eba.web;
import org.springframework.web.bind.annotation.*;
import ujc.projecto.eba.service.*;
import java.util.*;

/** Dados partilhados por todos os perfis autenticados (níveis e turmas cadastrados pelo administrador). */
@RestController @RequestMapping("/api/comum")
public class ComumController {
    private final AcademicoService ac;
    ComumController(AcademicoService ac) { this.ac = ac; }
    @GetMapping("/niveis") public List<Map<String,Object>> niveis() { return ac.niveis(); }
    @GetMapping("/disciplinas") public List<Map<String,Object>> disciplinas() { return ac.disciplinas(); }
    @GetMapping("/turmas") public List<Map<String,Object>> turmas(@RequestParam(required = false) String nivel) { return ac.turmas(nivel); }
}
