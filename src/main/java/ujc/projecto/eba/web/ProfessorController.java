package ujc.projecto.eba.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.service.*;
import java.util.*;
import static ujc.projecto.eba.service.Util.*;

@RestController @RequestMapping("/api/professor")
public class ProfessorController {
    private final AcademicoService ac; private final MaterialService mat;
    ProfessorController(AcademicoService ac, MaterialService mat) { this.ac = ac; this.mat = mat; }

    private static String disc(Utilizador u) { if (u.especialidade == null || u.especialidade.isBlank()) throw new RegraException(400, "O professor não tem disciplina definida. Peça ao administrador."); return u.especialidade; }

    @GetMapping("/turmas") public Map<String,Object> turmas(@RequestAttribute("u") Utilizador u) { return Map.of("disciplina", Objects.requireNonNullElse(u.especialidade, "Geral"), "turmas", ac.turmasDoProfessor(u.id)); }
    /** Lista de presenças (faltas). */
    @GetMapping("/turmas/{id}/alunos") public List<Map<String,Object>> alunos(@RequestAttribute("u") Utilizador u, @PathVariable Long id, @RequestParam(defaultValue = "3") int trimestre) { ac.exigirTurma(u.id, id); return ac.alunosDaTurma(id, disc(u), trimestre); }

    /** Pauta: testes do trimestre + notas por aluno + média do trimestre. */
    @GetMapping("/turmas/{id}/pauta") public Map<String,Object> pauta(@RequestAttribute("u") Utilizador u, @PathVariable Long id, @RequestParam(defaultValue = "1") int trimestre) { ac.exigirTurma(u.id, id); return ac.pauta(id, disc(u), trimestre); }
    @PostMapping("/testes") public Map<String,Object> novoTeste(@RequestAttribute("u") Utilizador u, @RequestBody Map<String,Object> b) {
        disc(u); Teste t = ac.criarTeste(l(b, "turmaId"), u, Integer.parseInt(s(b, "trimestre")), s(b, "titulo"), d(b, "data"));
        return Map.of("id", t.id, "titulo", t.titulo);
    }
    @DeleteMapping("/testes/{id}") public Map<String,Object> apagarTeste(@RequestAttribute("u") Utilizador u, @PathVariable Long id) { ac.apagarTeste(id, u); return Map.of("ok", true); }
    @PostMapping("/notas") public Map<String,Object> nota(@RequestAttribute("u") Utilizador u, @RequestBody Map<String,Object> b) {
        disc(u); Nota n = ac.lancarNota(l(b, "alunoId"), l(b, "testeId"), Double.parseDouble(s(b, "valor").replace(',', '.')), u);
        return Map.of("ok", true, "id", n.id);
    }
    @PostMapping("/faltas") @SuppressWarnings("unchecked")
    public Map<String,Object> faltas(@RequestAttribute("u") Utilizador u, @RequestBody Map<String,Object> b) {
        return Map.of("alertas", ac.registarFaltas(d(b, "data"), disc(u), (List<Map<String,Object>>) b.get("itens")));
    }

    /* ---- Material académico (upload) ---- */
    @GetMapping("/materiais") public List<Map<String,Object>> materiais(@RequestAttribute("u") Utilizador u) { return mat.doProfessor(u); }
    @PostMapping(value = "/materiais", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> publicar(@RequestAttribute("u") Utilizador u, @RequestParam Long turmaId, @RequestParam String titulo, @RequestParam(required = false) String descricao, @RequestParam("ficheiro") MultipartFile f) {
        return mat.view(mat.publicar(u, turmaId, titulo, descricao, f));
    }
    @PostMapping(value = "/materiais/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> atualizar(@RequestAttribute("u") Utilizador u, @PathVariable Long id, @RequestParam(required = false) String titulo, @RequestParam(required = false) String descricao, @RequestParam(value = "ficheiro", required = false) MultipartFile f) {
        return mat.view(mat.atualizar(u, id, titulo, descricao, f));
    }
    @GetMapping("/materiais/{id}/ficheiro")
    public ResponseEntity<byte[]> ficheiro(@RequestAttribute("u") Utilizador u, @PathVariable Long id) {
        Material m = mat.obter(id);
        if (!u.id.equals(m.professorId)) throw new RegraException(403, "Acesso negado.");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(m.nomeFicheiro, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(m.tipo)).body(mat.ler(m));
    }
    @DeleteMapping("/materiais/{id}") public Map<String,Object> apagar(@RequestAttribute("u") Utilizador u, @PathVariable Long id) { mat.apagar(u, id); return Map.of("ok", true); }
}
