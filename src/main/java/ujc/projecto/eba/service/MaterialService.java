package ujc.projecto.eba.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Material académico (PDF, DOCX, fotos…). Os ficheiros ficam em disco; qualquer novidade notifica os encarregados da turma. */
@Service
public class MaterialService {
    private static final Map<String,String> TIPOS = Map.ofEntries(
        Map.entry("pdf", "application/pdf"), Map.entry("doc", "application/msword"),
        Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Map.entry("ppt", "application/vnd.ms-powerpoint"), Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        Map.entry("xls", "application/vnd.ms-excel"), Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"), Map.entry("png", "image/png"));
    private final MaterialRepo repo; private final TurmaRepo turmas; private final MatriculaRepo mats; private final AlunoRepo alunos; private final AcademicoService ac;
    private final Path dir;

    public MaterialService(MaterialRepo r, TurmaRepo t, MatriculaRepo m, AlunoRepo a, AcademicoService ac, @Value("${eba.uploads.dir:uploads}") String dir) {
        repo = r; turmas = t; mats = m; alunos = a; this.ac = ac; this.dir = Paths.get(dir).toAbsolutePath().normalize();
    }
    private static String ext(String nome) { int i = nome == null ? -1 : nome.lastIndexOf('.'); return i < 0 ? "" : nome.substring(i + 1).toLowerCase(Locale.ROOT); }
    private static String limpar(String nome) { return nome == null ? "ficheiro" : nome.replaceAll("[\\\\/\\r\\n\"]", "_").trim(); }

    private Material guardarFicheiro(Material m, MultipartFile f) {
        if (f == null || f.isEmpty()) throw new RegraException(400, "Escolha um ficheiro (PDF, DOCX ou foto).");
        String e = ext(f.getOriginalFilename());
        if (!TIPOS.containsKey(e)) throw new RegraException(400, "Formato não permitido. Use PDF, DOC/DOCX, PPT/PPTX, XLS/XLSX, JPG ou PNG.");
        try {
            Files.createDirectories(dir);
            String nome = UUID.randomUUID() + "." + e; Path destino = dir.resolve(nome).normalize();
            if (!destino.startsWith(dir)) throw new RegraException(400, "Nome de ficheiro inválido.");
            f.transferTo(destino);
            if (m.caminho != null) Files.deleteIfExists(dir.resolve(m.caminho).normalize());      // substitui a versão anterior
            m.caminho = nome; m.nomeFicheiro = limpar(f.getOriginalFilename()); m.tipo = TIPOS.get(e); m.tamanho = f.getSize();
        } catch (IOException ex) { throw new RegraException(500, "Não foi possível guardar o ficheiro."); }
        return m;
    }
    public Map<String,Object> view(Material m) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id", m.id); r.put("turmaId", m.turmaId); r.put("turma", ac.nomeTurma(m.turmaId)); r.put("disciplina", m.disciplina); r.put("titulo", m.titulo);
        r.put("descricao", m.descricao); r.put("nomeFicheiro", m.nomeFicheiro); r.put("tipo", m.tipo); r.put("tamanho", m.tamanho); r.put("data", m.data); r.put("atualizadoEm", m.atualizadoEm);
        return r;
    }
    public List<Map<String,Object>> doProfessor(Utilizador p) { return repo.findByProfessorIdOrderByIdDesc(p.id).stream().map(this::view).collect(Collectors.toList()); }

    public Material publicar(Utilizador prof, Long turmaId, String titulo, String descricao, MultipartFile f) {
        ac.exigirTurma(prof.id, turmaId);
        Turma t = turmas.findById(turmaId).orElseThrow(() -> new RegraException(404, "Turma não encontrada."));
        if (titulo == null || titulo.isBlank()) throw new RegraException(400, "Indique o título do material.");
        Material m = new Material(); m.turmaId = turmaId; m.professorId = prof.id; m.disciplina = Objects.requireNonNullElse(prof.especialidade, "Geral");
        m.titulo = titulo.trim(); m.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim(); m.data = LocalDate.now();
        guardarFicheiro(m, f); m = repo.save(m);
        ac.notificarTurma(turmaId, "Novo material de " + m.disciplina + " para " + t.nivel + " – Turma " + t.codigo + ": \"" + m.titulo + "\".");
        return m;
    }
    public Material atualizar(Utilizador prof, Long id, String titulo, String descricao, MultipartFile f) {
        Material m = repo.findById(id).orElseThrow(() -> new RegraException(404, "Material não encontrado."));
        if (!prof.id.equals(m.professorId)) throw new RegraException(403, "Só pode alterar o seu próprio material.");
        if (titulo != null && !titulo.isBlank()) m.titulo = titulo.trim();
        if (descricao != null) m.descricao = descricao.isBlank() ? null : descricao.trim();
        if (f != null && !f.isEmpty()) guardarFicheiro(m, f);
        m.atualizadoEm = LocalDate.now(); m = repo.save(m);
        Turma t = turmas.findById(m.turmaId).orElse(null);
        ac.notificarTurma(m.turmaId, "Material atualizado em " + m.disciplina + (t == null ? "" : " (" + t.nivel + " – Turma " + t.codigo + ")") + ": \"" + m.titulo + "\".");
        return m;
    }
    public void apagar(Utilizador prof, Long id) {
        Material m = repo.findById(id).orElseThrow(() -> new RegraException(404, "Material não encontrado."));
        if (!prof.id.equals(m.professorId)) throw new RegraException(403, "Só pode apagar o seu próprio material.");
        try { if (m.caminho != null) Files.deleteIfExists(dir.resolve(m.caminho).normalize()); } catch (IOException ignorado) { }
        repo.delete(m);
    }
    /** Material das turmas dos educandos do encarregado (opcionalmente de um só aluno). */
    public List<Map<String,Object>> doEncarregado(Utilizador enc, Long alunoId) {
        Set<Long> turmasIds = new LinkedHashSet<>();
        for (Aluno a : alunos.findByEncarregadoId(enc.id)) {
            if (alunoId != null && !alunoId.equals(a.id)) continue;
            mats.findByAlunoId(a.id).forEach(x -> turmasIds.add(x.turmaId));
        }
        if (turmasIds.isEmpty()) return List.of();
        return repo.findByTurmaIdInOrderByIdDesc(turmasIds).stream().map(this::view).collect(Collectors.toList());
    }
    public boolean encarregadoPodeVer(Utilizador enc, Material m) {
        return alunos.findByEncarregadoId(enc.id).stream().anyMatch(a -> mats.existsByAlunoIdAndTurmaId(a.id, m.turmaId));
    }
    public Material obter(Long id) { return repo.findById(id).orElseThrow(() -> new RegraException(404, "Material não encontrado.")); }
    public byte[] ler(Material m) {
        try { return Files.readAllBytes(dir.resolve(m.caminho).normalize()); }
        catch (IOException e) { throw new RegraException(404, "Ficheiro indisponível."); }
    }
}
