package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MaterialRepo extends JpaRepository<Material,Long> {
 List<Material> findByTurmaIdOrderByIdDesc(Long t); List<Material> findByTurmaIdInOrderByIdDesc(Collection<Long> t); List<Material> findByProfessorIdOrderByIdDesc(Long p);
}
