package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface NotaRepo extends JpaRepository<Nota,Long> {
 List<Nota> findByAlunoId(Long id); Optional<Nota> findByAlunoIdAndTesteId(Long a, Long t); List<Nota> findByTesteId(Long t); void deleteByTesteId(Long t);
}
