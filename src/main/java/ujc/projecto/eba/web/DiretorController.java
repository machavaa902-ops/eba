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

@RestController @RequestMapping("/api/diretor")
public class DiretorController {
    private final UtilizadorRepo users; private final AlunoRepo alunos; private final TurmaRepo turmas; private final MensalidadeRepo mens; private final AcademicoService ac; private final FinanceiroService fin;
    DiretorController(UtilizadorRepo u, AlunoRepo a, TurmaRepo t, MensalidadeRepo m, AcademicoService ac, FinanceiroService f) { users = u; alunos = a; turmas = t; mens = m; this.ac = ac; fin = f; }

    private static Map<String,Object> prof(Utilizador u) { Map<String,Object> m = new LinkedHashMap<>(); m.put("id", u.id); m.put("nome", u.nome); m.put("email", u.email); m.put("contacto", u.contacto); m.put("especialidade", u.especialidade); m.put("dataContratacao", u.dataContratacao); m.put("ativo", u.ativo); return m; }

    @GetMapping("/professores") public List<Map<String,Object>> professores() { return users.findByPerfil("PROFESSOR").stream().map(DiretorController::prof).collect(Collectors.toList()); }
    /** UC03: cria/atualiza o professor e a respetiva conta de utilizador com perfil PROFESSOR. */
    @PostMapping("/professores") public Map<String,Object> contratar(@RequestBody Map<String,Object> b) { Utilizador u = new Utilizador(); u.perfil = "PROFESSOR"; return gravar(u, b, true); }
    @PutMapping("/professores/{id}") public Map<String,Object> editar(@PathVariable Long id, @RequestBody Map<String,Object> b) { return gravar(users.findById(id).orElseThrow(() -> new RegraException(404, "Professor não encontrado.")), b, false); }
    private Map<String,Object> gravar(Utilizador u, Map<String,Object> b, boolean novo) {
        List<String> falta = new ArrayList<>();
        for (String k : List.of("nome", "email", "especialidade", "dataContratacao", "contacto")) if (s(b, k) == null) falta.add(k);
        if (!falta.isEmpty()) throw new RegraException(400, "Dados obrigatórios em falta: " + String.join(", ", falta) + ".");
        String email = s(b, "email").toLowerCase();
        if (users.findByEmail(email).filter(x -> !x.id.equals(u.id)).isPresent()) throw new RegraException(409, "Já existe um utilizador com esse email.");
        u.nome = s(b, "nome"); u.email = email; u.contacto = s(b, "contacto"); u.especialidade = ac.exigirDisciplina(s(b, "especialidade")); u.dataContratacao = d(b, "dataContratacao");
        if (novo) Seguranca.definir(u, "1234");  // palavra-passe inicial (enviada por email na versão final)
        Utilizador g = users.save(u); ac.podarAtribuicoes(g);
        return prof(g);
    }
    @GetMapping("/estatisticas")
    public Map<String,Object> estatisticas() {
        LocalDate hoje = LocalDate.now(); double[] porMes = new double[12]; double receitaMes = 0, divida = 0;
        for (Mensalidade m : mens.findByAno(hoje.getYear())) {
            if ("PAGA".equals(m.estado)) { porMes[m.mes - 1] += m.valor + m.multa; if (m.mes == hoje.getMonthValue()) receitaMes += m.valor + m.multa; }
            else if (!"PENDENTE".equals(fin.estado(m))) divida += m.valor + fin.multa(m);
        }
        List<Map<String,Object>> top = alunos.findAll().stream().map(a -> { Map<String,Object> m = new LinkedHashMap<>(); m.put("nome", a.nome); m.put("turma", ac.nomeTurmas(a.id)); m.put("media", Util.r1(ac.mediaGeral(a.id))); return m; })
                .filter(m -> (double) m.get("media") > 0).sorted((x, y) -> Double.compare((double) y.get("media"), (double) x.get("media"))).limit(5).collect(Collectors.toList());
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("alunos", alunos.count()); r.put("turmas", turmas.count()); r.put("receitaMes", Util.r2(receitaMes)); r.put("dividas", Util.r2(divida));
        r.put("receitasMensais", porMes); r.put("melhoresAlunos", top); return r;
    }
    @GetMapping("/relatorio")
    public ResponseEntity<byte[]> relatorio(@RequestParam String tipo) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        if ("academico".equals(tipo)) {
            sb.append("Aluno;Turma;Media geral;Situacao\n");
            for (Aluno a : alunos.findAll()) { double md = ac.mediaGeral(a.id); sb.append(a.nome).append(';').append(ac.nomeTurmas(a.id)).append(';').append(Util.r1(md)).append(';').append(md == 0 ? "Sem notas" : AcademicoService.aprovado(md) ? "Aprovado" : "Nao aprovado").append('\n'); }
        } else {
            sb.append("Aluno;Mes;Ano;Valor;Multa;Estado;Recibo\n");
            for (Mensalidade m : mens.findAll()) { Map<String,Object> v = fin.view(m); String al = alunos.findById(m.alunoId).map(x -> x.nome).orElse("?");
                sb.append(al).append(';').append(v.get("mes")).append(';').append(m.ano).append(';').append(m.valor).append(';').append(v.get("multa")).append(';').append(v.get("estado")).append(';').append(m.recibo == null ? "" : m.recibo).append('\n'); }
        }
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=relatorio-" + tipo + ".csv").contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
