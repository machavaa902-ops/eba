package ujc.projecto.eba.service;
import ujc.projecto.eba.model.Utilizador;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
/** Palavras-passe guardadas apenas como hash com sal (RNF02). Em produção, preferir BCrypt/Argon2. */
public final class Seguranca {
    static String hash(String pw, String salt) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((salt + pw).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static void definir(Utilizador u, String pw) { u.salt = UUID.randomUUID().toString(); u.hash = hash(pw, u.salt); }
    public static boolean confere(String pw, Utilizador u) { return pw != null && hash(pw, u.salt).equals(u.hash); }
}
