package ujc.projecto.eba.model;
import jakarta.persistence.*;
/** Ligação aluno ↔ turma: um aluno pode pertencer a várias turmas. */
@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"alunoId", "turmaId"}))
public class Matricula {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long alunoId, turmaId;
}
