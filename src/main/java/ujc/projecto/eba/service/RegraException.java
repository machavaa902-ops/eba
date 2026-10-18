package ujc.projecto.eba.service;
/** Violação de regra de negócio / erro de validação, com o código HTTP a devolver. */
public class RegraException extends RuntimeException {
    public final int status;
    public RegraException(int status, String msg) { super(msg); this.status = status; }
}
