package ujc.projecto.eba.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static ujc.projecto.eba.service.Util.*;

@Service
public class AcademicoService {
    public static final int AULAS = 40, LIMITE = 10;               // RD05: 25% de 40 aulas
    private final AlunoRepo alunos; private final TurmaRepo turmas; private final NotaRepo notas; private final MatriculaRepo mats; private final TesteRepo testes;
    private final FaltaRepo faltas; private final NotificacaoRepo notifs; private final UtilizadorRepo users; private final NivelRepo niveis; private final AtribuicaoRepo atrib;
    private final DisciplinaRepo discs; private final TurmaDisciplinaRepo tds;

    public AcademicoService(AlunoRepo a, TurmaRepo t, NotaRepo n, MatriculaRepo m, TesteRepo te, FaltaRepo f, NotificacaoRepo no, UtilizadorRepo u, NivelRepo nv, AtribuicaoRepo at, DisciplinaRepo dc, TurmaDisciplinaRepo td) {
        discs = dc; tds = td; alunos = a; turmas = t; notas = n; mats = m; testes = te; faltas = f; notifs = no; users = u; niveis = nv; atrib = at;
    }
    public static int trimestre(LocalDate d) { int m = d.getMonthValue(); return m <= 4 ? 1 : m <= 8 ? 2 : 3; }
    public static boolean aprovado(double media) { return media >= 10; }   // RD02

    public void notificar(Long uid, String msg) {
        if (uid == null) return;
        Notificacao n = new Notificacao(); n.utilizadorId = uid; n.mensagem = msg; n.data = LocalDate.now(); notifs.save(n);
    }
    /** Notifica o encarregado de cada aluno da turma (uma vez por encarregado). */
    public int notificarTurma(Long turmaId, String msg) {
        Set<Long> encs = new LinkedHashSet<>();
        for (Aluno a : alunosDaTurmaEntidades(turmaId)) if (a.encarregadoId != null) encs.add(a.encarregadoId);
        encs.forEach(e -> notificar(e, msg));
        return encs.size();
    }
    public String nomeTurma(Long id) {
        return id == null ? "—" : turmas.findById(id).map(t -> t.nivel + " – Turma " + t.codigo).orElse("—");
    }
    public List<Turma> turmasDoAluno(Long alunoId) {
        return mats.findByAlunoId(alunoId).stream().map(m -> turmas.findById(m.turmaId).orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
    }
    public boolean matriculado(Long alunoId, Long turmaId) { return mats.existsByAlunoIdAndTurmaId(alunoId, turmaId); }
    public List<Aluno> alunosDaTurmaEntidades(Long turmaId) {
        List<Long> ids = mats.findByTurmaId(turmaId).stream().map(m -> m.alunoId).collect(Collectors.toList());
        return alunos.findAllById(ids);
    }
    public String nomeTurmas(Long alunoId) {
        String s = turmasDoAluno(alunoId).stream().map(t -> t.nivel + " – Turma " + t.codigo).collect(Collectors.joining(" · "));
        return s.isEmpty() ? "—" : s;
    }
    public Map<String,Object> view(Aluno a) {
        List<Turma> ts = turmasDoAluno(a.id);
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", a.id); m.put("nome", a.nome); m.put("nivel", a.nivel); m.put("estado", a.estado); m.put("genero", a.genero);
        m.put("nascimento", a.dataNascimento); m.put("encarregadoId", a.encarregadoId);
        m.put("turmaIds", ts.stream().map(t -> t.id).collect(Collectors.toList()));
        m.put("turmas", ts.stream().map(t -> t.nivel + " – Turma " + t.codigo).collect(Collectors.toList()));
        m.put("turma", ts.isEmpty() ? "—" : ts.stream().map(t -> t.nivel + " – Turma " + t.codigo).collect(Collectors.joining(" · ")));
        return m;
    }
    public Map<String,Object> turmaView(Turma t) {
        long oc = mats.countByTurmaId(t.id);
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", t.id); m.put("codigo", t.codigo); m.put("nivel", t.nivel); m.put("turno", t.turno); m.put("anoLetivo", t.anoLetivo);
        m.put("capacidade", t.capacidade); m.put("ocupacao", oc); m.put("vagas", Math.max(0, t.capacidade - oc)); m.put("label", t.nivel + " – Turma " + t.codigo);
        List<Disciplina> ds = disciplinasDaTurma(t.id); m.put("disciplinaIds", ds.stream().map(d -> d.id).collect(Collectors.toList())); m.put("disciplinas", ds.stream().map(d -> d.nome).collect(Collectors.toList()));
        return m;
    }
    public List<Map<String,Object>> turmas(String nivel) {
        return (nivel == null || nivel.isBlank() ? turmas.findAll() : turmas.findByNivel(nivel)).stream().map(this::turmaView).collect(Collectors.toList());
    }

    /* ---------- Níveis e turmas (administração) ---------- */
    public List<Map<String,Object>> niveis() {
        return niveis.findAllByOrderByOrdemAscNomeAsc().stream().map(n -> {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("id", n.id); m.put("nome", n.nome); m.put("turmas", turmas.countByNivel(n.nome)); return m; }).collect(Collectors.toList());
    }
    public Nivel novoNivel(String nome) {
        if (nome == null || nome.isBlank()) throw new RegraException(400, "Indique o nome do nível (ex.: 10.ª Classe).");
        nome = nome.trim();
        if (niveis.findByNome(nome).isPresent()) throw new RegraException(409, "Esse nível já está cadastrado.");
        Nivel n = new Nivel(); n.nome = nome; n.ordem = (int) niveis.count() + 1; return niveis.save(n);
    }
    public void eliminarNivel(Long id) {
        Nivel n = niveis.findById(id).orElseThrow(() -> new RegraException(404, "Nível não encontrado."));
        if (turmas.countByNivel(n.nome) > 0) throw new RegraException(409, "Existem turmas neste nível. Elimine-as primeiro.");
        niveis.delete(n);
    }
    public Turma guardarTurma(Turma t) {
        if (t.codigo == null || t.nivel == null || t.capacidade < 1) throw new RegraException(400, "Indique código, nível e capacidade.");
        if (niveis.findByNome(t.nivel).isEmpty()) throw new RegraException(400, "Nível inexistente. Cadastre-o primeiro no painel de administração.");
        Optional<Turma> dup = turmas.findByCodigoAndNivelAndAnoLetivo(t.codigo, t.nivel, t.anoLetivo);
        if (dup.isPresent() && !dup.get().id.equals(t.id)) throw new RegraException(409, "Já existe a turma " + t.codigo + " em " + t.nivel + " para " + t.anoLetivo + ".");
        if (t.id != null && mats.countByTurmaId(t.id) > t.capacidade) throw new RegraException(409, "A capacidade não pode ser inferior ao número de alunos já inscritos.");
        return turmas.save(t);
    }
    @Transactional
    public void eliminarTurma(Long id) {
        if (mats.countByTurmaId(id) > 0) throw new RegraException(409, "A turma tem alunos inscritos e não pode ser eliminada.");
        atrib.deleteByTurmaId(id); tds.deleteByTurmaId(id); turmas.deleteById(id);
    }

    /* ---------- Disciplinas (administração): professor → disciplina → turma ---------- */
    public List<Map<String,Object>> disciplinas() {
        List<Utilizador> profs = users.findByPerfil("PROFESSOR");
        return discs.findAllByOrderByNomeAsc().stream().map(d -> {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("id", d.id); m.put("nome", d.nome); m.put("turmas", tds.countByDisciplinaId(d.id));
            m.put("professores", profs.stream().filter(p -> d.nome.equalsIgnoreCase(p.especialidade)).count()); return m; }).collect(Collectors.toList());
    }
    public Disciplina novaDisciplina(String nome) {
        if (nome == null || nome.isBlank()) throw new RegraException(400, "Indique o nome da disciplina (ex.: Matemática).");
        nome = nome.trim();
        if (discs.findByNomeIgnoreCase(nome).isPresent()) throw new RegraException(409, "Essa disciplina já está cadastrada.");
        Disciplina d = new Disciplina(); d.nome = nome; return discs.save(d);
    }
    @Transactional
    public void eliminarDisciplina(Long id) {
        Disciplina d = discs.findById(id).orElseThrow(() -> new RegraException(404, "Disciplina não encontrada."));
        if (users.findByPerfil("PROFESSOR").stream().anyMatch(p -> d.nome.equalsIgnoreCase(p.especialidade))) throw new RegraException(409, "Há professores ligados a esta disciplina. Altere a disciplina deles primeiro.");
        tds.deleteByDisciplinaId(id); discs.delete(d);
    }
    /** Devolve o nome oficial da disciplina (a do cadastro) ou recusa se ela não existir. */
    public String exigirDisciplina(String nome) {
        if (nome == null || nome.isBlank()) throw new RegraException(400, "Indique a disciplina do professor.");
        return discs.findByNomeIgnoreCase(nome.trim()).map(d -> d.nome).orElseThrow(() -> new RegraException(400, "A disciplina \"" + nome.trim() + "\" não está cadastrada. Peça ao administrador para a cadastrar."));
    }
    public List<Disciplina> disciplinasDaTurma(Long turmaId) {
        return discs.findAllById(tds.findByTurmaId(turmaId).stream().map(x -> x.disciplinaId).collect(Collectors.toList())).stream().sorted(Comparator.comparing((Disciplina d) -> d.nome)).collect(Collectors.toList());
    }
    public boolean turmaTemDisciplina(Long turmaId, String nome) {
        return nome != null && disciplinasDaTurma(turmaId).stream().anyMatch(d -> d.nome.equalsIgnoreCase(nome));
    }
    /** Define as disciplinas leccionadas numa turma. Professores atribuídos à turma cuja disciplina deixou de existir nela são desligados. */
    @Transactional
    public int definirDisciplinasTurma(Long turmaId, List<Long> disciplinaIds) {
        if (!turmas.existsById(turmaId)) throw new RegraException(404, "Turma não encontrada.");
        Set<Long> novos = new LinkedHashSet<>(disciplinaIds == null ? List.<Long>of() : disciplinaIds);
        for (Long d : novos) if (!discs.existsById(d)) throw new RegraException(404, "Disciplina não encontrada.");
        for (TurmaDisciplina x : tds.findByTurmaId(turmaId)) if (!novos.contains(x.disciplinaId)) tds.delete(x);
        for (Long d : novos) if (!tds.existsByTurmaIdAndDisciplinaId(turmaId, d)) { TurmaDisciplina x = new TurmaDisciplina(); x.turmaId = turmaId; x.disciplinaId = d; tds.save(x); }
        int removidos = 0;
        Set<String> nomes = disciplinasDaTurma(turmaId).stream().map(d -> d.nome.toLowerCase()).collect(Collectors.toSet());
        for (Atribuicao a : atrib.findAll()) if (turmaId.equals(a.turmaId)) {
            Utilizador p = users.findById(a.professorId).orElse(null);
            if (p == null || p.especialidade == null || !nomes.contains(p.especialidade.toLowerCase())) { atrib.delete(a); removidos++; }
        }
        return removidos;
    }
    /** Depois de mudar a disciplina de um professor: desliga-o das turmas que não a têm. */
    @Transactional
    public void podarAtribuicoes(Utilizador p) {
        for (Atribuicao a : atrib.findByProfessorId(p.id)) if (!turmaTemDisciplina(a.turmaId, p.especialidade)) atrib.delete(a);
    }

    /* ---------- Turmas do professor (atribuídas pelo administrador) ---------- */
    public List<Long> turmaIdsProfessor(Long pid) {
        String disc = users.findById(pid).map(u -> u.especialidade).orElse(null);
        return atrib.findByProfessorId(pid).stream().map(a -> a.turmaId).filter(t -> turmaTemDisciplina(t, disc)).collect(Collectors.toList());
    }
    public List<Map<String,Object>> turmasDoProfessor(Long pid) {
        return turmaIdsProfessor(pid).stream().map(id -> turmas.findById(id).orElse(null)).filter(Objects::nonNull).map(this::turmaView).collect(Collectors.toList());
    }
    public void exigirTurma(Long pid, Long turmaId) {
        if (turmaId == null || !atrib.existsByProfessorIdAndTurmaId(pid, turmaId)) throw new RegraException(403, "Esta turma não lhe foi atribuída. Peça ao administrador.");
        String disc = users.findById(pid).map(u -> u.especialidade).orElse(null);
        if (!turmaTemDisciplina(turmaId, disc)) throw new RegraException(403, "A sua disciplina não é leccionada nesta turma. Peça ao administrador.");
    }
    @Transactional
    public void definirTurmasProfessor(Long pid, List<Long> turmaIds) {
        Utilizador p = users.findById(pid).orElseThrow(() -> new RegraException(404, "Professor não encontrado."));
        if (!"PROFESSOR".equals(p.perfil)) throw new RegraException(400, "Só professores têm turmas atribuídas.");
        Set<Long> novos = new LinkedHashSet<>(turmaIds == null ? List.<Long>of() : turmaIds);
        for (Long t : novos) {
            if (!turmas.existsById(t)) throw new RegraException(404, "Turma não encontrada.");
            if (!turmaTemDisciplina(t, p.especialidade)) throw new RegraException(409, "A disciplina " + p.especialidade + " não é leccionada na turma " + nomeTurma(t) + ". Associe-a primeiro à turma.");
        }
        for (Atribuicao a : atrib.findByProfessorId(pid)) if (!novos.contains(a.turmaId)) atrib.delete(a);
        for (Long t : novos) if (!atrib.existsByProfessorIdAndTurmaId(pid, t)) { Atribuicao a = new Atribuicao(); a.professorId = pid; a.turmaId = t; atrib.save(a); }
    }

    /* ---------- Registo de alunos (uma ou várias turmas) ---------- */
    private void validarTurmas(String nivel, Collection<Long> turmaIds, Long alunoId) {
        for (Long tid : turmaIds) {
            Turma t = turmas.findById(tid).orElseThrow(() -> new RegraException(404, "Turma não encontrada."));
            if (!t.nivel.equals(nivel)) throw new RegraException(409, "A turma " + t.nivel + " – " + t.codigo + " não é do mesmo nível/classe do aluno (RD08).");
            boolean jaEsta = alunoId != null && mats.existsByAlunoIdAndTurmaId(alunoId, tid);
            if (!jaEsta && mats.countByTurmaId(tid) >= t.capacidade) {
                String sug = turmas.findByNivel(nivel).stream().filter(x -> mats.countByTurmaId(x.id) < x.capacidade).map(x -> "Turma " + x.codigo).collect(Collectors.joining(", "));
                throw new RegraException(409, "Turma " + t.codigo + " sem vagas (RD07)." + (sug.isEmpty() ? " Não há outra turma disponível: proposta de lista de espera." : " Sugestão: " + sug + "."));
            }
        }
    }
    /** RD03 + RD07 + RD08. */
    @Transactional
    public Aluno registarAluno(Aluno a, List<Long> turmaIds, Long encId, String estado) {
        if (encId == null) throw new RegraException(400, "O encarregado de educação é obrigatório (RD03).");
        if (a.nome == null || a.nivel == null || turmaIds == null || turmaIds.isEmpty()) throw new RegraException(400, "Preencha nome, nível e escolha pelo menos uma turma.");
        Set<Long> ids = new LinkedHashSet<>(turmaIds);
        validarTurmas(a.nivel, ids, null);
        a.encarregadoId = encId; a.estado = estado; a.turmaId = null;
        Aluno g = alunos.save(a);
        for (Long tid : ids) { Matricula m = new Matricula(); m.alunoId = g.id; m.turmaId = tid; mats.save(m); }
        return g;
    }
    /** Substitui o conjunto de turmas de um aluno. */
    @Transactional
    public Aluno definirTurmas(Long alunoId, List<Long> turmaIds) {
        Aluno a = alunos.findById(alunoId).orElseThrow(() -> new RegraException(404, "Aluno não encontrado."));
        if (turmaIds == null || turmaIds.isEmpty()) throw new RegraException(400, "O aluno deve pertencer a pelo menos uma turma.");
        Set<Long> novos = new LinkedHashSet<>(turmaIds);
        validarTurmas(a.nivel, novos, a.id);
        for (Matricula m : mats.findByAlunoId(a.id)) if (!novos.contains(m.turmaId)) mats.delete(m);
        for (Long tid : novos) if (!mats.existsByAlunoIdAndTurmaId(a.id, tid)) { Matricula m = new Matricula(); m.alunoId = a.id; m.turmaId = tid; mats.save(m); }
        return a;
    }

    /* ---------- Testes e notas ---------- */
    @Transactional
    public Teste criarTeste(Long turmaId, Utilizador prof, int tri, String titulo, LocalDate data) {
        if (tri < 1 || tri > 3) throw new RegraException(400, "Trimestre inválido.");
        exigirTurma(prof.id, turmaId);
        if (turmas.findById(turmaId).isEmpty()) throw new RegraException(404, "Turma não encontrada.");
        List<Teste> ex = testes.findByTurmaIdAndDisciplinaAndTrimestreOrderByDataAscIdAsc(turmaId, prof.especialidade, tri);
        Teste t = new Teste(); t.turmaId = turmaId; t.professorId = prof.id; t.disciplina = prof.especialidade; t.trimestre = tri;
        t.titulo = titulo == null ? "Teste " + (ex.size() + 1) : titulo; t.data = data == null ? LocalDate.now() : data;
        return testes.save(t);
    }
    @Transactional
    public void apagarTeste(Long testeId, Utilizador prof) {
        Teste t = testes.findById(testeId).orElseThrow(() -> new RegraException(404, "Teste não encontrado."));
        exigirTurma(prof.id, t.turmaId);
        if (!Objects.equals(t.disciplina, prof.especialidade)) throw new RegraException(403, "Só pode apagar testes da sua disciplina.");
        notas.deleteByTesteId(testeId); testes.delete(t);
    }
    /** RD01: nota entre 0 e 20. Notifica o encarregado quando a nota é nova ou muda. */
    @Transactional
    public Nota lancarNota(Long alunoId, Long testeId, double v, Utilizador prof) {
        if (v < 0 || v > 20) throw new RegraException(400, "Nota fora do intervalo permitido (0 a 20 valores) – RD01.");
        Teste t = testes.findById(testeId).orElseThrow(() -> new RegraException(404, "Teste não encontrado."));
        exigirTurma(prof.id, t.turmaId);
        if (!Objects.equals(t.disciplina, prof.especialidade)) throw new RegraException(403, "Só pode lançar notas da sua disciplina.");
        Aluno a = alunos.findById(alunoId).orElseThrow(() -> new RegraException(404, "Aluno não encontrado."));
        if (!mats.existsByAlunoIdAndTurmaId(alunoId, t.turmaId)) throw new RegraException(409, "O aluno não pertence à turma deste teste.");
        Optional<Nota> ex = notas.findByAlunoIdAndTesteId(alunoId, testeId);
        boolean mudou = ex.isEmpty() || ex.get().valor != v;
        Nota n = ex.orElseGet(Nota::new); n.alunoId = alunoId; n.testeId = testeId; n.disciplina = t.disciplina; n.trimestre = t.trimestre; n.valor = v;
        n = notas.save(n);
        if (mudou) notificar(a.encarregadoId, "Nova nota de " + a.nome + " em " + t.disciplina + " (" + t.titulo + ", " + t.trimestre + ".º T): " + r1(v) + " valores.");
        return n;
    }
    private static double media(Collection<Double> v) { return v.stream().mapToDouble(Double::doubleValue).average().orElse(0); }

    /** Pauta do professor: testes do trimestre, notas por aluno e média do trimestre (média dos testes). */
    public Map<String,Object> pauta(Long turmaId, String disc, int tri) {
        List<Teste> ts = testes.findByTurmaIdAndDisciplinaAndTrimestreOrderByDataAscIdAsc(turmaId, disc, tri);
        List<Map<String,Object>> lt = ts.stream().map(t -> { Map<String,Object> m = new LinkedHashMap<>(); m.put("id", t.id); m.put("titulo", t.titulo); m.put("data", t.data); return m; }).collect(Collectors.toList());
        List<Map<String,Object>> la = new ArrayList<>();
        for (Aluno a : alunosDaTurmaEntidades(turmaId)) {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("id", a.id); m.put("nome", a.nome); m.put("estado", a.estado);
            Map<String,Object> nm = new LinkedHashMap<>(); List<Double> todas = new ArrayList<>(); Double legado = null;
            for (Nota n : notas.findByAlunoId(a.id)) {
                if (!n.disciplina.equals(disc) || n.trimestre != tri) continue;
                if (n.testeId == null) { legado = n.valor; todas.add(n.valor); }
                else if (ts.stream().anyMatch(t -> t.id.equals(n.testeId))) { nm.put(String.valueOf(n.testeId), n.valor); todas.add(n.valor); }
            }
            m.put("notas", nm); m.put("legado", legado); m.put("media", todas.isEmpty() ? null : r1(media(todas)));
            la.add(m);
        }
        la.sort(Comparator.comparing(x -> String.valueOf(x.get("nome"))));
        Map<String,Object> out = new LinkedHashMap<>(); out.put("testes", lt); out.put("alunos", la); return out;
    }

    /** RD04: média do trimestre = média dos testes desse trimestre; média final = média dos trimestres com notas. RD02: aprovado se >= 10. */
    public List<Map<String,Object>> boletim(Long alunoId) {
        Map<String,List<Nota>> por = notas.findByAlunoId(alunoId).stream().collect(Collectors.groupingBy(n -> n.disciplina, TreeMap::new, Collectors.toList()));
        List<Map<String,Object>> out = new ArrayList<>();
        por.forEach((d, l) -> {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("disciplina", d);
            List<Double> medias = new ArrayList<>();
            for (int tri = 1; tri <= 3; tri++) {
                final int tt = tri;
                List<Double> v = l.stream().filter(n -> n.trimestre == tt).map(n -> n.valor).collect(Collectors.toList());
                if (v.isEmpty()) continue;
                double mt = media(v); m.put("t" + tri, r1(mt)); m.put("n" + tri, v.size()); medias.add(mt);
            }
            double md = media(medias);
            m.put("media", r1(md)); m.put("aprovado", aprovado(md)); out.add(m);
        });
        return out;
    }
    /** Todos os testes/notas do aluno, para o detalhe no portal do encarregado. */
    public List<Map<String,Object>> testesDoAluno(Long alunoId) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (Nota n : notas.findByAlunoId(alunoId)) {
            Teste t = n.testeId == null ? null : testes.findById(n.testeId).orElse(null);
            Map<String,Object> m = new LinkedHashMap<>(); m.put("disciplina", n.disciplina); m.put("trimestre", n.trimestre);
            m.put("titulo", t == null ? "Teste" : t.titulo); m.put("data", t == null ? null : t.data); m.put("valor", n.valor); out.add(m);
        }
        out.sort(Comparator.comparing((Map<String,Object> x) -> String.valueOf(x.get("disciplina"))).thenComparing(x -> (Integer) x.get("trimestre")));
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
        m.put("boletim", boletim(a.id)); m.put("mediaGeral", r1(mediaGeral(a.id))); m.put("testes", testesDoAluno(a.id));
        m.put("faltas", faltasPorDisciplina(a.id, trimestre(LocalDate.now()))); m.put("trimestre", trimestre(LocalDate.now()));
        return m;
    }
    /** Lista de presenças da turma (faltas do trimestre por aluno). */
    public List<Map<String,Object>> alunosDaTurma(Long turmaId, String disc, int tri) {
        List<Map<String,Object>> out = new ArrayList<>();
        int ano = LocalDate.now().getYear(), ini = tri == 1 ? 1 : tri == 2 ? 5 : 9;
        LocalDate i = LocalDate.of(ano, ini, 1), f = LocalDate.of(ano, ini + 3, 1).plusMonths(1).minusDays(1);
        for (Aluno a : alunosDaTurmaEntidades(turmaId)) {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("id", a.id); m.put("nome", a.nome); m.put("estado", a.estado);
            m.put("faltas", faltas.countByAlunoIdAndDisciplinaAndDataBetween(a.id, disc, i, f)); m.put("limite", LIMITE); m.put("aulas", AULAS);
            out.add(m);
        }
        out.sort(Comparator.comparing(x -> String.valueOf(x.get("nome"))));
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
