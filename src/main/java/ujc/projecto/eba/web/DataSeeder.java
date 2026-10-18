package ujc.projecto.eba.web;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ujc.projecto.eba.model.*;
import ujc.projecto.eba.repo.*;
import ujc.projecto.eba.service.*;
import java.time.LocalDate;
import java.util.*;

/** Dados de demonstração (só se a base estiver vazia) + garantias idempotentes para bases já existentes
 *  (administrador, níveis, contas de pagamento e migração aluno→turma para a tabela de matrículas). */
@Component
class DataSeeder implements CommandLineRunner {
    private final UtilizadorRepo users; private final TurmaRepo turmas; private final AlunoRepo alunos; private final NotaRepo notas; private final MensalidadeRepo mens; private final FinanceiroService fin;
    private final NivelRepo niveis; private final MatriculaRepo mats; private final ContaPagamentoRepo contas; private final AtribuicaoRepo atrib; private final DisciplinaRepo discs; private final TurmaDisciplinaRepo tds;
    DataSeeder(UtilizadorRepo u, TurmaRepo t, AlunoRepo a, NotaRepo n, MensalidadeRepo m, FinanceiroService f, NivelRepo nv, MatriculaRepo mt, ContaPagamentoRepo c, AtribuicaoRepo at, DisciplinaRepo dc, TurmaDisciplinaRepo td) { discs = dc; tds = td; users = u; turmas = t; alunos = a; notas = n; mens = m; fin = f; niveis = nv; mats = mt; contas = c; atrib = at; }

    private Utilizador u(String nome, String email, String perfil, String esp) {
        Utilizador x = new Utilizador(); x.nome = nome; x.email = email; x.perfil = perfil; x.especialidade = esp; x.contacto = "+258 84 512 3398"; x.dataContratacao = esp == null ? null : LocalDate.of(2024, 2, 1);
        Seguranca.definir(x, "1234"); return users.save(x);
    }
    private Turma t(String c, String n, int cap) { Turma x = new Turma(); x.codigo = c; x.nivel = n; x.capacidade = cap; x.anoLetivo = LocalDate.now().getYear(); x.turno = "Manhã"; return turmas.save(x); }
    private Aluno a(String nome, Turma t, Utilizador e) {
        Aluno x = new Aluno(); x.nome = nome; x.nivel = t.nivel; x.encarregadoId = e.id; x.estado = "ATIVO"; x.genero = "Masculino"; x.dataNascimento = LocalDate.of(2012, 5, 14); x = alunos.save(x);
        Matricula m = new Matricula(); m.alunoId = x.id; m.turmaId = t.id; mats.save(m); return x;
    }

    @Override public void run(String... args) {
        if (users.count() == 0) demo();
        garantir();
    }

    private void demo() {
        u("Amílcar Sitoe", "diretor@eba.co.mz", "DIRETOR", null); u("Marta Cossa", "secretaria@eba.co.mz", "SECRETARIA", null);
        u("Carlos Macuácua", "professor@eba.co.mz", "PROFESSOR", "Matemática"); u("Luísa Mondlane", "luisa@eba.co.mz", "PROFESSOR", "Português");
        Utilizador helena = u("Helena Nhantumbo", "encarregado@eba.co.mz", "ENCARREGADO", null), outro = u("Alberto Mucavele", "alberto@eba.co.mz", "ENCARREGADO", null);
        for (String n : List.of("6.ª Classe", "7.ª Classe", "8.ª Classe", "9.ª Classe")) { Nivel nv = new Nivel(); nv.nome = n; nv.ordem = (int) niveis.count() + 1; niveis.save(nv); }
        Turma a8 = t("A", "8.ª Classe", 30), b8 = t("B", "8.ª Classe", 30), c6 = t("A", "6.ª Classe", 1), a7 = t("A", "7.ª Classe", 30);
        Aluno ana = a("Ana Carolina Nhantumbo", a8, helena), bruno = a("Bruno Filipe Machava", a8, outro), nelio = a("Nélio Augusto Nhantumbo", c6, helena), dercio = a("Dércio Alberto Mucavele", b8, outro), edna = a("Edna Jorge Chongo", a7, outro);
        // notas de demonstração (1 por trimestre); o professor pode acrescentar quantos testes quiser
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

    /** Corre sempre: não duplica nada e não mexe no que já existe. */
    private void garantir() {
        if (users.findByEmail("admin@eba.co.mz").isEmpty()) u("Administrador do Sistema", "admin@eba.co.mz", "ADMIN", null);
        // níveis: os já usados por turmas/alunos + os 4 base
        Set<String> nomes = new LinkedHashSet<>(List.of("6.ª Classe", "7.ª Classe", "8.ª Classe", "9.ª Classe"));
        turmas.findAll().forEach(x -> { if (x.nivel != null) nomes.add(x.nivel); });
        for (String n : nomes) if (niveis.findByNome(n).isEmpty()) { Nivel nv = new Nivel(); nv.nome = n; nv.ordem = (int) niveis.count() + 1; niveis.save(nv); }
        // contas de depósito (valores de exemplo: o administrador altera no painel)
        String[][] cs = {{"MPESA", "Centro Ebenezer Academy", "84 000 0000", null}, {"EMOLA", "Centro Ebenezer Academy", "86 000 0000", null}, {"TRANSFERENCIA", "Centro Ebenezer Academy", "0000 0000 0000 0000 0", "Banco (preencher)"}};
        for (String[] c : cs) if (contas.findByMetodo(c[0]).isEmpty()) { ContaPagamento x = new ContaPagamento(); x.metodo = c[0]; x.titular = c[1]; x.numero = c[2]; x.banco = c[3]; contas.save(x); }
        // primeira vez com atribuições: mantém o comportamento anterior (cada professor fica com todas as turmas); depois o admin ajusta
        if (atrib.count() == 0) for (Utilizador p : users.findByPerfil("PROFESSOR")) for (Turma t : turmas.findAll()) { Atribuicao a = new Atribuicao(); a.professorId = p.id; a.turmaId = t.id; atrib.save(a); }
        // disciplinas: as dos professores existentes entram no cadastro; cada atribuição professor→turma passa a significar "a disciplina dele está nessa turma"
        for (Utilizador p : users.findByPerfil("PROFESSOR")) {
            if (p.especialidade == null || p.especialidade.isBlank()) continue;
            Disciplina d = discs.findByNomeIgnoreCase(p.especialidade.trim()).orElseGet(() -> { Disciplina n = new Disciplina(); n.nome = p.especialidade.trim(); return discs.save(n); });
            for (Atribuicao a : atrib.findByProfessorId(p.id)) if (!tds.existsByTurmaIdAndDisciplinaId(a.turmaId, d.id)) { TurmaDisciplina x = new TurmaDisciplina(); x.turmaId = a.turmaId; x.disciplinaId = d.id; tds.save(x); }
        }
        // migração: aluno.turmaId (modelo antigo) → matrícula
        for (Aluno al : alunos.findAll()) if (al.turmaId != null) {
            if (turmas.existsById(al.turmaId) && !mats.existsByAlunoIdAndTurmaId(al.id, al.turmaId)) { Matricula m = new Matricula(); m.alunoId = al.id; m.turmaId = al.turmaId; mats.save(m); }
            al.turmaId = null; alunos.save(al);
        }
    }
}
