package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface NotificacaoRepo extends JpaRepository<Notificacao,Long> {
 List<Notificacao> findByUtilizadorIdOrderByIdDesc(Long id);
}
