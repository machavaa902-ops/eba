package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MatriculaRepo extends JpaRepository<Matricula,Long> {
 List<Matricula> findByAlunoId(Long a); List<Matricula> findByTurmaId(Long t); long countByTurmaId(Long t);
 boolean existsByAlunoIdAndTurmaId(Long a, Long t); void deleteByAlunoIdAndTurmaId(Long a, Long t);
}
