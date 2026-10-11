package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Falta {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long alunoId; public String disciplina, tipo; public LocalDate data;
}
