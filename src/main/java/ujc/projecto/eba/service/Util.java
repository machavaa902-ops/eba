package ujc.projecto.eba.service;
import java.time.LocalDate;
import java.util.Map;
public final class Util {
    public static String s(Map<String,?> m, String k) { Object o = m.get(k); return o == null || o.toString().isBlank() ? null : o.toString().trim(); }
    public static Long l(Map<String,?> m, String k) { String v = s(m, k); return v == null ? null : Long.valueOf(v); }
    public static LocalDate d(Map<String,?> m, String k) { String v = s(m, k); return v == null ? null : LocalDate.parse(v); }
    public static java.util.List<Long> ids(Map<String,?> m, String k) {
        Object o = m.get(k); java.util.List<Long> out = new java.util.ArrayList<>();
        if (o instanceof java.util.Collection<?> c) for (Object x : c) { if (x != null && !x.toString().isBlank()) out.add(Long.valueOf(x.toString().trim())); }
        else if (o != null && !o.toString().isBlank()) out.add(Long.valueOf(o.toString().trim()));
        return out;
    }
    public static double r1(double v) { return Math.round(v * 10) / 10.0; }
    public static double r2(double v) { return Math.round(v * 100) / 100.0; }
}
