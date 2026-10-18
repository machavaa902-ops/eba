package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TesteRepo extends JpaRepository<Teste,Long> {
 List<Teste> findByTurmaIdAndDisciplinaAndTrimestreOrderByDataAscIdAsc(Long t, String d, int tri);
}
