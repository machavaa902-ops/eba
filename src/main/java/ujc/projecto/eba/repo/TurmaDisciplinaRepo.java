package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TurmaDisciplinaRepo extends JpaRepository<TurmaDisciplina,Long> {
 List<TurmaDisciplina> findByTurmaId(Long t); long countByDisciplinaId(Long d); boolean existsByTurmaIdAndDisciplinaId(Long t, Long d);
 void deleteByTurmaId(Long t); void deleteByDisciplinaId(Long d);
}
