package ujc.projecto.eba.model;
import jakarta.persistence.*;
/** Turma atribuída a um professor (feita pelo administrador). */
@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"professorId", "turmaId"}))
public class Atribuicao {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long professorId, turmaId;
}
