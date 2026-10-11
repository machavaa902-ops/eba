package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface MensalidadeRepo extends JpaRepository<Mensalidade,Long> {
 List<Mensalidade> findByAlunoIdOrderByAnoAscMesAsc(Long id); List<Mensalidade> findByAnoAndMes(int a,int m); List<Mensalidade> findByAno(int a); Optional<Mensalidade> findByAlunoIdAndAnoAndMes(Long a,int y,int m);
}
