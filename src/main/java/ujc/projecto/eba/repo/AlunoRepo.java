package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface AlunoRepo extends JpaRepository<Aluno,Long> {
 List<Aluno> findByEncarregadoId(Long id); List<Aluno> findByTurmaId(Long id); List<Aluno> findByEstado(String e); long countByTurmaId(Long id);
}
