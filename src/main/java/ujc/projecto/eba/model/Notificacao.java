package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Notificacao {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long utilizadorId; public String mensagem; public LocalDate data; public boolean lida;
}
