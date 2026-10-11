package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Nota {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long alunoId; public String disciplina; public int trimestre; public double valor;
}
