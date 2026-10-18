package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
/** Teste/avaliação criado pelo professor (quantos quiser, em cada trimestre). */
@Entity public class Teste {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long turmaId, professorId; public String disciplina, titulo; public int trimestre; public LocalDate data;
}
