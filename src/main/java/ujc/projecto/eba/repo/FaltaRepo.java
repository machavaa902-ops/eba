package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface FaltaRepo extends JpaRepository<Falta,Long> {
 Optional<Falta> findByAlunoIdAndDisciplinaAndData(Long a,String d,LocalDate dt); long countByAlunoIdAndDisciplinaAndDataBetween(Long a,String d,LocalDate i,LocalDate f); List<Falta> findByAlunoId(Long id);
}
