package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AtribuicaoRepo extends JpaRepository<Atribuicao,Long> {
 List<Atribuicao> findByProfessorId(Long p); boolean existsByProfessorIdAndTurmaId(Long p, Long t); void deleteByTurmaId(Long t);
}
