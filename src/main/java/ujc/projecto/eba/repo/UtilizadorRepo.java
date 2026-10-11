package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface UtilizadorRepo extends JpaRepository<Utilizador,Long> {
 Optional<Utilizador> findByEmail(String e); List<Utilizador> findByPerfil(String p);
}
