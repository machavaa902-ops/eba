package ujc.projecto.eba.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import ujc.projecto.eba.service.*;
import java.util.*;
import java.util.stream.Collectors;
import static ujc.projecto.eba.service.Util.*;

@RestController @RequestMapping("/api/encarregado")
public class EncarregadoController {
    private final AlunoRepo alunos; private final MensalidadeRepo mens; private final NotificacaoRepo notifs; private final UtilizadorRepo users;
    private final AcademicoService ac; private final FinanceiroService fin; private final MaterialService mat;
    EncarregadoController(AlunoRepo a, MensalidadeRepo m, NotificacaoRepo n, UtilizadorRepo us, AcademicoService ac, FinanceiroService f, MaterialService mat) { alunos = a; mens = m; notifs = n; users = us; this.ac = ac; fin = f; this.mat = mat; }

    private Aluno meu(Utilizador u, Long id) { return alunos.findById(id).filter(a -> u.id.equals(a.encarregadoId)).orElseThrow(() -> new RegraException(403, "Acesso negado.")); }

    @GetMapping("/educandos") public List<Map<String,Object>> educandos(@RequestAttribute("u") Utilizador u) { return alunos.findByEncarregadoId(u.id).stream().map(ac::view).collect(Collectors.toList()); }
    @GetMapping("/alunos/{id}/resumo") public Map<String,Object> resumo(@RequestAttribute("u") Utilizador u, @PathVariable Long id) { return ac.resumo(meu(u, id)); }
    @GetMapping("/alunos/{id}/pagamentos") public List<Map<String,Object>> pagamentos(@RequestAttribute("u") Utilizador u, @PathVariable Long id) { return fin.extrato(meu(u, id).id); }
    @GetMapping("/turmas") public List<Map<String,Object>> turmas(@RequestParam String nivel) { return ac.turmas(nivel); }

    /** Nome e número da conta onde o encarregado deve depositar (M-Pesa, e-Mola, banco). */
    @GetMapping("/contas-pagamento") public List<Map<String,Object>> contas() { return fin.contas(); }

    @PostMapping("/pagamentos")
    public Map<String,Object> pagar(@RequestAttribute("u") Utilizador u, @RequestBody Map<String,Object> b) {
        Mensalidade m = mens.findById(l(b, "mensalidadeId")).orElseThrow(() -> new RegraException(404, "Mensalidade não encontrada."));
        meu(u, m.alunoId);
        return fin.view(fin.pagar(m.id, s(b, "metodo"), s(b, "numero")));
    }
    /** UC04: inscrição online com estado "pendente de validação" e secretária notificada. Aceita várias turmas. */
    @PostMapping("/inscricoes")
    public Map<String,Object> inscrever(@RequestAttribute("u") Utilizador u, @RequestBody Map<String,Object> b) {
        Aluno a = new Aluno(); a.nome = s(b, "nome"); a.nivel = s(b, "nivel"); a.genero = s(b, "genero"); a.dataNascimento = d(b, "nascimento");
        a = ac.registarAluno(a, ids(b, "turmaIds"), u.id, "PENDENTE");
        for (Utilizador sec : users.findByPerfil("SECRETARIA")) ac.notificar(sec.id, "Nova inscrição online pendente: " + a.nome + ".");
        return ac.view(a);
    }
    @GetMapping("/notificacoes") public List<Notificacao> notificacoes(@RequestAttribute("u") Utilizador u) { return notifs.findByUtilizadorIdOrderByIdDesc(u.id); }

    /** Material académico publicado pelos professores para as turmas dos educandos. */
    @GetMapping("/materiais") public List<Map<String,Object>> materiais(@RequestAttribute("u") Utilizador u, @RequestParam(required = false) Long alunoId) {
        if (alunoId != null) meu(u, alunoId);
        return mat.doEncarregado(u, alunoId);
    }
    @GetMapping("/materiais/{id}/ficheiro")
    public ResponseEntity<byte[]> ficheiro(@RequestAttribute("u") Utilizador u, @PathVariable Long id) {
        Material m = mat.obter(id);
        if (!mat.encarregadoPodeVer(u, m)) throw new RegraException(403, "Acesso negado.");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(m.nomeFicheiro, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(m.tipo)).body(mat.ler(m));
    }
}
