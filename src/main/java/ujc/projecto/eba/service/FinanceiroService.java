package ujc.projecto.eba.service;
import org.springframework.stereotype.Service;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static ujc.projecto.eba.service.Util.*;

@Service
public class FinanceiroService {
    public static final double VALOR = 2500, TAXA_MULTA = 0.10; public static final int DIA_LIMITE = 8;   // RD06
    public static final String[] MESES = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho","Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
    private final MensalidadeRepo mens; private final AlunoRepo alunos; private final AcademicoService ac; private final PagamentoGateway gw; private final ContaPagamentoRepo contas;

    public FinanceiroService(MensalidadeRepo m, AlunoRepo a, AcademicoService ac, PagamentoGateway gw, ContaPagamentoRepo c) { mens = m; alunos = a; this.ac = ac; this.gw = gw; contas = c; }

    /** Contas (nome + número) para onde o encarregado deve depositar, por método de pagamento. */
    public List<Map<String,Object>> contas() {
        List<String> ordem = List.of("MPESA", "EMOLA", "TRANSFERENCIA");
        return contas.findAll().stream().sorted(Comparator.comparingInt(c -> ordem.indexOf(c.metodo))).map(c -> {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("id", c.id); m.put("metodo", c.metodo); m.put("titular", c.titular); m.put("numero", c.numero); m.put("banco", c.banco); return m; }).collect(Collectors.toList());
    }

    public static LocalDate vencimento(Mensalidade m) { return LocalDate.of(m.ano, m.mes, DIA_LIMITE); }
    public double multa(Mensalidade m) {
        if ("PAGA".equals(m.estado)) return m.multa;
        return LocalDate.now().isAfter(vencimento(m)) ? r2(m.valor * TAXA_MULTA) : 0;
    }
    public String estado(Mensalidade m) {
        return "PAGA".equals(m.estado) ? "PAGA" : LocalDate.now().isAfter(vencimento(m)) ? "ATRASADA" : "PENDENTE";
    }
    public Map<String,Object> view(Mensalidade m) {
        double mu = multa(m);
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id", m.id); r.put("alunoId", m.alunoId); r.put("mes", MESES[m.mes - 1]); r.put("ano", m.ano); r.put("vencimento", vencimento(m));
        r.put("valor", m.valor); r.put("multa", mu); r.put("total", r2(m.valor + mu)); r.put("estado", estado(m));
        r.put("pagoEm", m.dataPagamento); r.put("recibo", m.recibo); r.put("metodo", m.metodo);
        return r;
    }
    public List<Map<String,Object>> extrato(Long alunoId) {
        return mens.findByAlunoIdOrderByAnoAscMesAsc(alunoId).stream().map(this::view).collect(Collectors.toList());
    }
    public int gerar(int mes, int ano) {
        int n = 0;
        for (Aluno a : alunos.findAll()) {
            if (!"ATIVO".equals(a.estado) && !"REPROVADO_FALTAS".equals(a.estado)) continue;
            if (mens.findByAlunoIdAndAnoAndMes(a.id, ano, mes).isPresent()) continue;
            Mensalidade m = new Mensalidade(); m.alunoId = a.id; m.mes = mes; m.ano = ano; m.valor = VALOR; m.estado = "PENDENTE"; mens.save(m); n++;
        }
        return n;
    }
    /** UC05/RF14: aplica multa (RD06), cobra no gateway, atualiza estado e emite recibo (RD09). */
    public Mensalidade pagar(Long id, String metodo, String numero) {
        Mensalidade m = mens.findById(id).orElseThrow(() -> new RegraException(404, "Mensalidade não encontrada."));
        if ("PAGA".equals(m.estado)) throw new RegraException(409, "Esta mensalidade já está paga.");
        if (!List.of("MPESA", "EMOLA", "TRANSFERENCIA", "PRESENCIAL").contains(metodo)) throw new RegraException(400, "Método de pagamento inválido.");
        double mu = multa(m);
        if (!"PRESENCIAL".equals(metodo) && !gw.cobrar(metodo, numero, m.valor + mu))
            throw new RegraException(402, "Transação recusada pelo gateway. Tente novamente ou escolha outro método.");
        m.multa = mu; m.estado = "PAGA"; m.dataPagamento = LocalDate.now(); m.metodo = metodo;
        final Mensalidade g = mens.save(m);
        g.recibo = String.format("R-%d-%05d", g.ano, g.id); mens.save(g);
        alunos.findById(g.alunoId).ifPresent(a -> ac.notificar(a.encarregadoId, "Pagamento de " + MESES[g.mes - 1] + " confirmado. Recibo " + g.recibo + "."));
        return g;
    }
}
