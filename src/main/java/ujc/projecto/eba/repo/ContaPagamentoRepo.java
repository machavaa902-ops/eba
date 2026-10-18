package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ContaPagamentoRepo extends JpaRepository<ContaPagamento,Long> { Optional<ContaPagamento> findByMetodo(String m); }
