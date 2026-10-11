package ujc.projecto.eba.service;
import org.springframework.stereotype.Component;
/** Simulação: recusa números inválidos ou terminados em 0000 (para testar CT22). */
@Component
public class GatewaySimulado implements PagamentoGateway {
    public boolean cobrar(String metodo, String numero, double valor) {
        if ("TRANSFERENCIA".equals(metodo)) return true;
        String n = numero == null ? "" : numero.replaceAll("\\D", "");
        return n.length() >= 9 && !n.endsWith("0000");
    }
}
