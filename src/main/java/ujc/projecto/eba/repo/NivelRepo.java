package ujc.projecto.eba.repo;
import ujc.projecto.eba.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NivelRepo extends JpaRepository<Nivel,Long> { Optional<Nivel> findByNome(String n); List<Nivel> findAllByOrderByOrdemAscNomeAsc(); }
