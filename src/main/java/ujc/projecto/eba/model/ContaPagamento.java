package ujc.projecto.eba.model;
import jakarta.persistence.*;
/** Conta de destino do depósito (M-Pesa, e-Mola, banco) mostrada ao encarregado no pagamento. */
@Entity public class ContaPagamento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true) public String metodo; public String titular, numero, banco;
}
