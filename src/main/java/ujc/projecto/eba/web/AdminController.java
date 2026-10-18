package ujc.projecto.eba.web;
import org.springframework.web.bind.annotation.*;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import ujc.projecto.eba.service.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static ujc.projecto.eba.service.Util.*;

/** Painel de administração do sistema: níveis, turmas, utilizadores e contas de pagamento. */
@RestController @RequestMapping("/api/admin")
public class AdminController {
    static final List<String> PERFIS = List.of("ADMIN", "DIRETOR", "SECRETARIA", "PROFESSOR", "ENCARREGADO");
    private final AcademicoService ac; private final UtilizadorRepo users; private final TurmaRepo turmas; private final AlunoRepo alunos; private final ContaPagamentoRepo contas;
    AdminController(AcademicoService ac, UtilizadorRepo u, TurmaRepo t, AlunoRepo a, ContaPagamentoRepo c) { this.ac = ac; users = u; turmas = t; alunos = a; contas = c; }

    /* ---- Níveis ---- */
    @GetMapping("/niveis") public List<Map<String,Object>> niveis() { return ac.niveis(); }
    @PostMapping("/niveis") public Map<String,Object> novoNivel(@RequestBody Map<String,Object> b) { Nivel n = ac.novoNivel(s(b, "nome")); return Map.of("id", n.id, "nome", n.nome); }
    @DeleteMapping("/niveis/{id}") public Map<String,Object> eliminarNivel(@PathVariable Long id) { ac.eliminarNivel(id); return Map.of("ok", true); }

    /* ---- Disciplinas ---- */
    @GetMapping("/disciplinas") public List<Map<String,Object>> disciplinas() { return ac.disciplinas(); }
    @PostMapping("/disciplinas") public Map<String,Object> novaDisciplina(@RequestBody Map<String,Object> b) { Disciplina d = ac.novaDisciplina(s(b, "nome")); return Map.of("id", d.id, "nome", d.nome); }
    @DeleteMapping("/disciplinas/{id}") public Map<String,Object> eliminarDisciplina(@PathVariable Long id) { ac.eliminarDisciplina(id); return Map.of("ok", true); }
    /** Define as disciplinas leccionadas numa turma. */
    @PutMapping("/turmas/{id}/disciplinas") public Map<String,Object> disciplinasTurma(@PathVariable Long id, @RequestBody Map<String,Object> b) { return Map.of("ok", true, "professoresDesligados", ac.definirDisciplinasTurma(id, ids(b, "disciplinaIds"))); }

    /* ---- Turmas ---- */
    @GetMapping("/turmas") public List<Map<String,Object>> turmas() { return ac.turmas(null); }
    @PostMapping("/turmas") public Map<String,Object> novaTurma(@RequestBody Map<String,Object> b) { return ac.turmaView(ac.guardarTurma(preencher(new Turma(), b))); }
    @PutMapping("/turmas/{id}") public Map<String,Object> editarTurma(@PathVariable Long id, @RequestBody Map<String,Object> b) {
        return ac.turmaView(ac.guardarTurma(preencher(turmas.findById(id).orElseThrow(() -> new RegraException(404, "Turma não encontrada.")), b)));
    }
    @DeleteMapping("/turmas/{id}") public Map<String,Object> eliminarTurma(@PathVariable Long id) { ac.eliminarTurma(id); return Map.of("ok", true); }
    private static Turma preencher(Turma t, Map<String,Object> b) {
        t.codigo = s(b, "codigo"); t.nivel = s(b, "nivel"); t.turno = Objects.requireNonNullElse(s(b, "turno"), "Manhã");
        t.anoLetivo = Integer.parseInt(Objects.requireNonNullElse(s(b, "anoLetivo"), "" + LocalDate.now().getYear()));
        t.capacidade = Integer.parseInt(Objects.requireNonNullElse(s(b, "capacidade"), "30")); return t;
    }

    /* ---- Utilizadores ---- */
    private Map<String,Object> uv(Utilizador u) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("id", u.id); m.put("nome", u.nome); m.put("email", u.email); m.put("perfil", u.perfil);
        m.put("contacto", u.contacto); m.put("especialidade", u.especialidade); m.put("ativo", u.ativo);
        if ("PROFESSOR".equals(u.perfil)) m.put("turmaIds", ac.turmaIdsProfessor(u.id));
        return m;
    }
    @GetMapping("/utilizadores") public List<Map<String,Object>> utilizadores() { return users.findAll().stream().sorted(Comparator.comparing((Utilizador u) -> u.perfil).thenComparing(u -> u.nome)).map(this::uv).collect(Collectors.toList()); }
    @PostMapping("/utilizadores") public Map<String,Object> novoUtilizador(@RequestBody Map<String,Object> b) {
        String perfil = s(b, "perfil"), email = s(b, "email") == null ? null : s(b, "email").toLowerCase();
        if (s(b, "nome") == null || email == null || perfil == null) throw new RegraException(400, "Indique nome, email e perfil.");
        if (!PERFIS.contains(perfil)) throw new RegraException(400, "Perfil inválido.");
        if ("PROFESSOR".equals(perfil)) b.put("especialidade", ac.exigirDisciplina(s(b, "especialidade")));
        if (users.findByEmail(email).isPresent()) throw new RegraException(409, "Já existe um utilizador com esse email.");
        Utilizador u = new Utilizador(); u.nome = s(b, "nome"); u.email = email; u.perfil = perfil; u.contacto = s(b, "contacto"); u.especialidade = s(b, "especialidade");
        if ("PROFESSOR".equals(perfil)) u.dataContratacao = LocalDate.now();
        Seguranca.definir(u, "1234");
        return uv(users.save(u));
    }
    @PutMapping("/utilizadores/{id}") public Map<String,Object> editarUtilizador(@RequestAttribute("u") Utilizador eu, @PathVariable Long id, @RequestBody Map<String,Object> b) {
        Utilizador u = users.findById(id).orElseThrow(() -> new RegraException(404, "Utilizador não encontrado."));
        if (s(b, "nome") != null) u.nome = s(b, "nome");
        if (b.containsKey("contacto")) u.contacto = s(b, "contacto");
        if (b.containsKey("especialidade")) u.especialidade = "PROFESSOR".equals(u.perfil) ? ac.exigirDisciplina(s(b, "especialidade")) : s(b, "especialidade");
        if (b.containsKey("ativo")) {
            boolean ativo = Boolean.parseBoolean(String.valueOf(b.get("ativo")));
            if (!ativo && u.id.equals(eu.id)) throw new RegraException(409, "Não pode desativar a sua própria conta.");
            u.ativo = ativo;
        }
        u = users.save(u);
        if ("PROFESSOR".equals(u.perfil) && b.containsKey("especialidade")) ac.podarAtribuicoes(u);
        return uv(u);
    }
    /** Atribui turmas a um professor: ele só vê e mexe nas turmas atribuídas. */
    @PutMapping("/utilizadores/{id}/turmas") public Map<String,Object> turmasProfessor(@PathVariable Long id, @RequestBody Map<String,Object> b) {
        ac.definirTurmasProfessor(id, ids(b, "turmaIds")); return Map.of("ok", true);
    }
    @PostMapping("/utilizadores/{id}/redefinir-senha") public Map<String,Object> redefinir(@PathVariable Long id) {
        Utilizador u = users.findById(id).orElseThrow(() -> new RegraException(404, "Utilizador não encontrado."));
        Seguranca.definir(u, "1234"); users.save(u); return Map.of("mensagem", "Palavra-passe redefinida para 1234.");
    }

    /* ---- Contas de pagamento (nome + número do depósito) ---- */
    @GetMapping("/contas") public List<ContaPagamento> contas() { return contas.findAll(); }
    @PutMapping("/contas/{id}") public ContaPagamento editarConta(@PathVariable Long id, @RequestBody Map<String,Object> b) {
        ContaPagamento c = contas.findById(id).orElseThrow(() -> new RegraException(404, "Conta não encontrada."));
        if (s(b, "titular") == null || s(b, "numero") == null) throw new RegraException(400, "Indique o nome do titular e o número.");
        c.titular = s(b, "titular"); c.numero = s(b, "numero"); c.banco = s(b, "banco"); return contas.save(c);
    }

    @GetMapping("/resumo") public Map<String,Object> resumo() {
        return Map.of("utilizadores", users.count(), "alunos", alunos.count(), "turmas", turmas.count(), "niveis", ac.niveis().size());
    }
}
