package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Utilizador {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String nome; @Column(unique=true) public String email; public String hash, salt, perfil, contacto, especialidade; public LocalDate dataContratacao; public boolean ativo = true;
}
