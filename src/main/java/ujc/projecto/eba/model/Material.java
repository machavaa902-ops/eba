package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
/** Material académico (PDF, DOCX, fotos) publicado pelo professor para uma turma. */
@Entity public class Material {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long turmaId, professorId; public String disciplina, titulo, nomeFicheiro, tipo, caminho;
 @Column(length = 1000) public String descricao; public LocalDate data, atualizadoEm; public long tamanho;
}
