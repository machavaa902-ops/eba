package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DisciplinaRepo extends JpaRepository<Disciplina,Long> { Optional<Disciplina> findByNomeIgnoreCase(String n); List<Disciplina> findAllByOrderByNomeAsc(); }
