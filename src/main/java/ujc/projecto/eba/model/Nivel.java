package ujc.projecto.eba.model;
import jakarta.persistence.*;
/** Nível / classe (ex.: "8.ª Classe"), cadastrado pelo administrador. */
@Entity public class Nivel {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true) public String nome; public int ordem;
}
