package ujc.projecto.eba.web;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import ujc.projecto.eba.service.*;
import java.time.LocalDate;

/** Dados de demonstração (só se a base estiver vazia). Todas as contas usam a palavra-passe 1234. */
@Component
class DataSeeder implements CommandLineRunner {
    private final UtilizadorRepo users; private final TurmaRepo turmas; private final AlunoRepo alunos; private final NotaRepo notas; private final MensalidadeRepo mens; private final FinanceiroService fin;
    DataSeeder(UtilizadorRepo u, TurmaRepo t, AlunoRepo a, NotaRepo n, MensalidadeRepo m, FinanceiroService f) { users = u; turmas = t; alunos = a; notas = n; mens = m; fin = f; }

    private Utilizador u(String nome, String email, String perfil, String esp) {
        Utilizador x = new Utilizador(); x.nome = nome; x.email = email; x.perfil = perfil; x.especialidade = esp; x.contacto = "+258 84 512 3398"; x.dataContratacao = esp == null ? null : LocalDate.of(2024, 2, 1);
        Seguranca.definir(x, "1234"); return users.save(x);
    }
    private Turma t(String c, String n, int cap) { Turma x = new Turma(); x.codigo = c; x.nivel = n; x.capacidade = cap; x.anoLetivo = LocalDate.now().getYear(); x.turno = "Manhã"; return turmas.save(x); }
    private Aluno a(String nome, Turma t, Utilizador e) { Aluno x = new Aluno(); x.nome = nome; x.nivel = t.nivel; x.turmaId = t.id; x.encarregadoId = e.id; x.estado = "ATIVO"; x.genero = "Masculino"; x.dataNascimento = LocalDate.of(2012, 5, 14); return alunos.save(x); }

    @Override public void run(String... args) {
        if (users.count() > 0) return;
        u("Amílcar Sitoe", "diretor@eba.co.mz", "DIRETOR", null); u("Marta Cossa", "secretaria@eba.co.mz", "SECRETARIA", null);
        u("Carlos Macuácua", "professor@eba.co.mz", "PROFESSOR", "Matemática"); u("Luísa Mondlane", "luisa@eba.co.mz", "PROFESSOR", "Português");
        Utilizador helena = u("Helena Nhantumbo", "encarregado@eba.co.mz", "ENCARREGADO", null), outro = u("Alberto Mucavele", "alberto@eba.co.mz", "ENCARREGADO", null);
        Turma a8 = t("A", "8.ª Classe", 30), b8 = t("B", "8.ª Classe", 30), c6 = t("A", "6.ª Classe", 1), a7 = t("A", "7.ª Classe", 30);
        Aluno ana = a("Ana Carolina Nhantumbo", a8, helena), bruno = a("Bruno Filipe Machava", a8, outro), nelio = a("Nélio Augusto Nhantumbo", c6, helena), dercio = a("Dércio Alberto Mucavele", b8, outro), edna = a("Edna Jorge Chongo", a7, outro);
        Object[][] nt = {{ana, 14, 13, 15}, {bruno, 15, 14, 16}, {dercio, 9, 10, 8}, {edna, 18, 17, 19}};
        for (Object[] r : nt) for (String d : new String[]{"Matemática", "Português"}) for (int i = 1; i <= 3; i++) {
            Nota n = new Nota(); n.alunoId = ((Aluno) r[0]).id; n.disciplina = d; n.trimestre = i; n.valor = (int) r[i] + (d.equals("Português") ? -1 : 0); notas.save(n); }
        LocalDate hoje = LocalDate.now();
        for (Aluno al : alunos.findAll()) for (int mes = 1; mes <= hoje.getMonthValue(); mes++) {
            Mensalidade m = new Mensalidade(); m.alunoId = al.id; m.mes = mes; m.ano = hoje.getYear(); m.valor = FinanceiroService.VALOR; m.estado = "PENDENTE";
            if (mes < hoje.getMonthValue()) { m.estado = "PAGA"; m.dataPagamento = FinanceiroService.vencimento(m); m.metodo = "MPESA"; }
            mens.save(m); if ("PAGA".equals(m.estado)) { m.recibo = String.format("R-%d-%05d", m.ano, m.id); mens.save(m); }
        }
    }
}
