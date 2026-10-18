package ujc.projecto.eba.model;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Mensalidade {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long alunoId; public int mes, ano; public double valor, multa; public String estado, metodo, recibo; public LocalDate dataPagamento;
}
