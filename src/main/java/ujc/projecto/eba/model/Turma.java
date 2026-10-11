package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Turma {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String codigo, nivel, turno; public int anoLetivo, capacidade;
}
