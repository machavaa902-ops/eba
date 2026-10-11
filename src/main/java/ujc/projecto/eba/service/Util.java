package ujc.projecto.eba.service;
import java.time.LocalDate;
import java.util.Map;
public final class Util {
    public static String s(Map<String,?> m, String k) { Object o = m.get(k); return o == null || o.toString().isBlank() ? null : o.toString().trim(); }
    public static Long l(Map<String,?> m, String k) { String v = s(m, k); return v == null ? null : Long.valueOf(v); }
    public static LocalDate d(Map<String,?> m, String k) { String v = s(m, k); return v == null ? null : LocalDate.parse(v); }
    public static double r1(double v) { return Math.round(v * 10) / 10.0; }
    public static double r2(double v) { return Math.round(v * 100) / 100.0; }
}
