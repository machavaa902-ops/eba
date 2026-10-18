package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Aluno {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String nome, genero, nivel, estado; public LocalDate dataNascimento; public Long turmaId /* LEGADO: migrado para Matricula no arranque */, encarregadoId;
}
