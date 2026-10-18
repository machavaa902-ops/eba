package ujc.projecto.eba;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "Aplicação EBA está funcionando! 🚀";
    }

    @GetMapping("/hello")
    public String hello() {
        return "Olá, mundo!";
    }
}