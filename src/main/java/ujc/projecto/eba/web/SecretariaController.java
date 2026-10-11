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

@RestController @RequestMapping("/api/secretaria")
public class SecretariaController {
    private final AlunoRepo alunos; private final TurmaRepo turmas; private final MensalidadeRepo mens; private final UtilizadorRepo users; private final NotificacaoRepo notifs;
    private final AcademicoService ac; private final FinanceiroService fin;
    SecretariaController(AlunoRepo a, TurmaRepo t, MensalidadeRepo m, UtilizadorRepo u, NotificacaoRepo n, AcademicoService ac, FinanceiroService f) { alunos = a; turmas = t; mens = m; users = u; notifs = n; this.ac = ac; fin = f; }

    @GetMapping("/turmas") public List<Map<String,Object>> turmas() { return ac.turmas(null); }
    @PostMapping("/turmas") public Map<String,Object> novaTurma(@RequestBody Map<String,Object> b) {
        Turma t = new Turma(); t.codigo = s(b, "codigo"); t.nivel = s(b, "nivel"); t.turno = s(b, "turno"); t.anoLetivo = Integer.parseInt(Objects.requireNonNullElse(s(b, "anoLetivo"), "" + LocalDate.now().getYear()));
        t.capacidade = Integer.parseInt(Objects.requireNonNullElse(s(b, "capacidade"), "30"));
        if (t.codigo == null || t.nivel == null || t.capacidade < 1) throw new RegraException(400, "Indique código, nível e capacidade.");
        return ac.turmaView(turmas.save(t));
    }
    @GetMapping("/alunos") public List<Map<String,Object>> alunos(@RequestParam(required = false) String estado) {
        return (estado == null ? alunos.findAll() : alunos.findByEstado(estado)).stream().map(a -> {
            Map<String,Object> m = new LinkedHashMap<>(ac.view(a));
            Utilizador e = a.encarregadoId == null ? null : users.findById(a.encarregadoId).orElse(null);
            m.put("encarregado", e == null ? "—" : e.nome); return m; }).collect(Collectors.toList());
    }
    /** UC02: RD03 (encarregado obrigatório), RD07 (vaga) e RD08 (nível compatível). */
    @PostMapping("/alunos") @SuppressWarnings("unchecked")
    public Map<String,Object> novoAluno(@RequestBody Map<String,Object> b) {
        Map<String,Object> e = b.get("encarregado") instanceof Map ? (Map<String,Object>) b.get("encarregado") : Map.of();
        Long encId = null;
        if (s(e, "email") != null && s(e, "nome") != null) {
            Utilizador enc = users.findByEmail(s(e, "email").toLowerCase()).orElseGet(() -> {
                Utilizador n = new Utilizador(); n.nome = s(e, "nome"); n.email = s(e, "email").toLowerCase(); n.contacto = s(e, "contacto"); n.perfil = "ENCARREGADO";
                Seguranca.definir(n, "1234"); return users.save(n); });
            encId = enc.id;
        }
        Aluno a = new Aluno(); a.nome = s(b, "nome"); a.nivel = s(b, "nivel"); a.genero = s(b, "genero"); a.dataNascimento = d(b, "nascimento");
        return ac.view(ac.registarAluno(a, l(b, "turmaId"), encId, "ATIVO"));
    }
    @PostMapping("/inscricoes/{id}/validar") public Map<String,Object> validar(@PathVariable Long id) {
        Aluno a = alunos.findById(id).orElseThrow(() -> new RegraException(404, "Aluno não encontrado."));
        a.estado = "ATIVO"; alunos.save(a); ac.notificar(a.encarregadoId, "A inscrição de " + a.nome + " foi validada."); return ac.view(a);
    }
    @GetMapping("/mensalidades") public Map<String,Object> mensalidades(@RequestParam int mes, @RequestParam int ano) {
        double recebido = 0; int pagas = 0, pend = 0, atraso = 0; List<Map<String,Object>> rows = new ArrayList<>();
        for (Mensalidade m : mens.findByAnoAndMes(ano, mes)) {
            Map<String,Object> v = new LinkedHashMap<>(fin.view(m)); Aluno a = alunos.findById(m.alunoId).orElse(null);
            v.put("aluno", a == null ? "?" : a.nome); v.put("turma", a == null ? "—" : ac.nomeTurma(a.turmaId)); rows.add(v);
            switch ((String) v.get("estado")) { case "PAGA" -> { pagas++; recebido += m.valor + m.multa; } case "ATRASADA" -> atraso++; default -> pend++; }
        }
        return Map.of("recebido", Util.r2(recebido), "pagas", pagas, "pendentes", pend, "atrasadas", atraso, "linhas", rows);
    }
    @PostMapping("/mensalidades/gerar") public Map<String,Object> gerar(@RequestBody Map<String,Object> b) { return Map.of("geradas", fin.gerar(Integer.parseInt(s(b, "mes")), Integer.parseInt(s(b, "ano")))); }
    @PostMapping("/mensalidades/{id}/pagar") public Map<String,Object> pagarPresencial(@PathVariable Long id) { return fin.view(fin.pagar(id, "PRESENCIAL", null)); }
    @GetMapping("/notificacoes") public List<Notificacao> notificacoes(@RequestAttribute("u") Utilizador u) { return notifs.findByUtilizadorIdOrderByIdDesc(u.id); }
}
