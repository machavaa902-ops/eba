package ujc.projecto.eba.model;
import jakarta.persistence.*;
/** Ligação disciplina ↔ turma: indica que a disciplina é leccionada nessa turma. */
@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"turmaId", "disciplinaId"}))
public class TurmaDisciplina {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long turmaId, disciplinaId;
}
