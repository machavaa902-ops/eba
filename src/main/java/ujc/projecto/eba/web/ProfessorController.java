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

@RestController @RequestMapping("/api/professor")
public class ProfessorController {
    private final AlunoRepo alunos; private final AcademicoService ac;
    ProfessorController(AlunoRepo a, AcademicoService ac) { alunos = a; this.ac = ac; }

    @GetMapping("/turmas") public Map<String,Object> turmas(@RequestAttribute("u") Utilizador u) { return Map.of("disciplina", Objects.requireNonNullElse(u.especialidade, "Geral"), "turmas", ac.turmas(null)); }
    @GetMapping("/turmas/{id}/alunos") public List<Map<String,Object>> alunos(@RequestAttribute("u") Utilizador u, @PathVariable Long id, @RequestParam(defaultValue = "3") int trimestre) { return ac.alunosDaTurma(id, u.especialidade, trimestre); }
    @PostMapping("/notas") public Map<String,Object> nota(@RequestAttribute("u") Utilizador u, @RequestBody Map<String,Object> b) {
        Nota n = ac.lancarNota(l(b, "alunoId"), Integer.parseInt(s(b, "trimestre")), Double.parseDouble(s(b, "valor")), u.especialidade);
        return Map.of("ok", true, "media", Util.r1(ac.boletim(n.alunoId).stream().filter(x -> x.get("disciplina").equals(u.especialidade)).mapToDouble(x -> (double) x.get("media")).findFirst().orElse(0)));
    }
    @PostMapping("/faltas") @SuppressWarnings("unchecked")
    public Map<String,Object> faltas(@RequestAttribute("u") Utilizador u, @RequestBody Map<String,Object> b) {
        return Map.of("alertas", ac.registarFaltas(d(b, "data"), u.especialidade, (List<Map<String,Object>>) b.get("itens")));
    }
}
