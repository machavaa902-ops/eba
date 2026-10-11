package ujc.projecto.eba.service;
import org.springframework.stereotype.Service;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static ujc.projecto.eba.service.Util.*;

@Service
public class AcademicoService {
    public static final int AULAS = 40, LIMITE = 10;               // RD05: 25% de 40 aulas
    private final AlunoRepo alunos; private final TurmaRepo turmas; private final NotaRepo notas;
    private final FaltaRepo faltas; private final NotificacaoRepo notifs; private final UtilizadorRepo users;

    public AcademicoService(AlunoRepo a, TurmaRepo t, NotaRepo n, FaltaRepo f, NotificacaoRepo no, UtilizadorRepo u) {
        alunos = a; turmas = t; notas = n; faltas = f; notifs = no; users = u;
    }
    public static int trimestre(LocalDate d) { int m = d.getMonthValue(); return m <= 4 ? 1 : m <= 8 ? 2 : 3; }
    public static boolean aprovado(double media) { return media >= 10; }   // RD02

    public void notificar(Long uid, String msg) {
        if (uid == null) return;
        Notificacao n = new Notificacao(); n.utilizadorId = uid; n.mensagem = msg; n.data = LocalDate.now(); notifs.save(n);
    }
    public String nomeTurma(Long id) {
        return id == null ? "—" : turmas.findById(id).map(t -> t.nivel + " – Turma " + t.codigo).orElse("—");
    }
    public Map<String,Object> view(Aluno a) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", a.id); m.put("nome", a.nome); m.put("nivel", a.nivel); m.put("estado", a.estado); m.put("genero", a.genero);
        m.put("nascimento", a.dataNascimento); m.put("turmaId", a.turmaId); m.put("turma", nomeTurma(a.turmaId)); m.put("encarregadoId", a.encarregadoId);
        return m;
    }
    public Map<String,Object> turmaView(Turma t) {
        long oc = alunos.countByTurmaId(t.id);
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", t.id); m.put("codigo", t.codigo); m.put("nivel", t.nivel); m.put("turno", t.turno); m.put("anoLetivo", t.anoLetivo);
        m.put("capacidade", t.capacidade); m.put("ocupacao", oc); m.put("vagas", Math.max(0, t.capacidade - oc)); m.put("label", t.nivel + " – Turma " + t.codigo);
        return m;
    }
    public List<Map<String,Object>> turmas(String nivel) {
        return (nivel == null ? turmas.findAll() : turmas.findByNivel(nivel)).stream().map(this::turmaView).collect(Collectors.toList());
    }
    /** RD03 + RD07 + RD08. */
    public Aluno registarAluno(Aluno a, Long turmaId, Long encId, String estado) {
        if (encId == null) throw new RegraException(400, "O encarregado de educação é obrigatório (RD03).");
        if (a.nome == null || a.nivel == null || turmaId == null) throw new RegraException(400, "Preencha nome, nível e turma.");
        Turma t = turmas.findById(turmaId).orElseThrow(() -> new RegraException(404, "Turma não encontrada."));
        if (!t.nivel.equals(a.nivel)) throw new RegraException(409, "A turma escolhida não é do mesmo nível/classe do aluno (RD08).");
        if (alunos.countByTurmaId(turmaId) >= t.capacidade) {
            String sug = turmas.findByNivel(a.nivel).stream().filter(x -> alunos.countByTurmaId(x.id) < x.capacidade).map(x -> "Turma " + x.codigo).collect(Collectors.joining(", "));
            throw new RegraException(409, "Turma sem vagas (RD07)." + (sug.isEmpty() ? " Não há outra turma disponível: proposta de lista de espera." : " Sugestão: " + sug + "."));
        }
        a.turmaId = turmaId; a.encarregadoId = encId; a.estado = estado;
        return alunos.save(a);
    }
    /** RD01: nota entre 0 e 20. */
    public Nota lancarNota(Long alunoId, int tri, double v, String disc) {
        if (v < 0 || v > 20) throw new RegraException(400, "Nota fora do intervalo permitido (0 a 20 valores) – RD01.");
        if (tri < 1 || tri > 3) throw new RegraException(400, "Trimestre inválido.");
        Nota n = notas.findByAlunoIdAndDisciplinaAndTrimestre(alunoId, disc, tri).orElseGet(Nota::new);
        n.alunoId = alunoId; n.disciplina = disc; n.trimestre = tri; n.valor = v;
        return notas.save(n);
    }
    /** RD04: média dos trimestres lançados; RD02: aprovado se >= 10. */
    public List<Map<String,Object>> boletim(Long alunoId) {
        Map<String,List<Nota>> por = notas.findByAlunoId(alunoId).stream().collect(Collectors.groupingBy(n -> n.disciplina, TreeMap::new, Collectors.toList()));
        List<Map<String,Object>> out = new ArrayList<>();
        por.forEach((d, l) -> {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("disciplina", d);
            for (Nota n : l) m.put("t" + n.trimestre, n.valor);
            double media = l.stream().mapToDouble(n -> n.valor).average().orElse(0);
            m.put("media", r1(media)); m.put("aprovado", aprovado(media)); out.add(m);
        });
        return out;
    }
    public double mediaGeral(Long alunoId) {
        return boletim(alunoId).stream().mapToDouble(m -> (double) m.get("media")).average().orElse(0);
    }
    public List<Map<String,Object>> faltasPorDisciplina(Long alunoId, int tri) {
        int ano = LocalDate.now().getYear(), ini = tri == 1 ? 1 : tri == 2 ? 5 : 9;
        LocalDate i = LocalDate.of(ano, ini, 1), f = LocalDate.of(ano, ini + 3, 1).plusMonths(1).minusDays(1);
        List<Map<String,Object>> out = new ArrayList<>();
        faltas.findByAlunoId(alunoId).stream().map(x -> x.disciplina).distinct().forEach(d -> {
            long c = faltas.countByAlunoIdAndDisciplinaAndDataBetween(alunoId, d, i, f);
            out.add(Map.of("disciplina", d, "faltas", c, "limite", LIMITE, "aulas", AULAS, "acima", c > LIMITE));
        });
        return out;
    }
    public Map<String,Object> resumo(Aluno a) {
        Map<String,Object> m = new LinkedHashMap<>(view(a));
        m.put("boletim", boletim(a.id)); m.put("mediaGeral", r1(mediaGeral(a.id)));
        m.put("faltas", faltasPorDisciplina(a.id, trimestre(LocalDate.now()))); m.put("trimestre", trimestre(LocalDate.now()));
        return m;
    }
    public List<Map<String,Object>> alunosDaTurma(Long turmaId, String disc, int tri) {
        List<Map<String,Object>> out = new ArrayList<>();
        int ano = LocalDate.now().getYear(), ini = tri == 1 ? 1 : tri == 2 ? 5 : 9;
        LocalDate i = LocalDate.of(ano, ini, 1), f = LocalDate.of(ano, ini + 3, 1).plusMonths(1).minusDays(1);
        for (Aluno a : alunos.findByTurmaId(turmaId)) {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("id", a.id); m.put("nome", a.nome); m.put("estado", a.estado);
            for (Nota n : notas.findByAlunoId(a.id)) if (n.disciplina.equals(disc)) m.put("t" + n.trimestre, n.valor);
            m.put("faltas", faltas.countByAlunoIdAndDisciplinaAndDataBetween(a.id, disc, i, f)); m.put("limite", LIMITE); m.put("aulas", AULAS);
            out.add(m);
        }
        return out;
    }
    /** RD05: mais de 25% das aulas → "reprovado por faltas" e notificação ao encarregado. */
    public List<String> registarFaltas(LocalDate data, String disc, List<Map<String,Object>> itens) {
        int tri = trimestre(data), ini = tri == 1 ? 1 : tri == 2 ? 5 : 9;
        LocalDate i = LocalDate.of(data.getYear(), ini, 1), f = LocalDate.of(data.getYear(), ini + 3, 1).plusMonths(1).minusDays(1);
        List<String> alertas = new ArrayList<>();
        for (Map<String,Object> it : itens) {
            Long id = l(it, "alunoId"); String tipo = s(it, "tipo");
            Optional<Falta> ex = faltas.findByAlunoIdAndDisciplinaAndData(id, disc, data);
            if ("PRESENTE".equals(tipo)) { ex.ifPresent(faltas::delete); continue; }
            Falta fa = ex.orElseGet(Falta::new); fa.alunoId = id; fa.disciplina = disc; fa.data = data; fa.tipo = tipo; faltas.save(fa);
            long total = faltas.countByAlunoIdAndDisciplinaAndDataBetween(id, disc, i, f);
            Aluno a = alunos.findById(id).orElse(null);
            if (a != null && total > LIMITE && !"REPROVADO_FALTAS".equals(a.estado)) {
                a.estado = "REPROVADO_FALTAS"; alunos.save(a);
                notificar(a.encarregadoId, a.nome + " ultrapassou 25% das aulas de " + disc + " e foi assinalado como reprovado por faltas.");
                alertas.add(a.nome + " ultrapassou 25% das aulas (" + total + " de " + AULAS + ").");
            }
        }
        return alertas;
    }
}
