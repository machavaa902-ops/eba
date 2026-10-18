package ujc.projecto.eba.model;
import jakarta.persistence.*;
/** Disciplina leccionada no centro (ex.: "Matemática"), cadastrada pelo administrador. */
@Entity public class Disciplina {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true) public String nome;
}
