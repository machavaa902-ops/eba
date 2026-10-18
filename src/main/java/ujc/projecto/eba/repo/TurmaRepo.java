package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface TurmaRepo extends JpaRepository<Turma,Long> {
 List<Turma> findByNivel(String n); long countByNivel(String n); Optional<Turma> findByCodigoAndNivelAndAnoLetivo(String c, String n, int a);
}
