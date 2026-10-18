package ujc.projecto.eba.service;
/** Adaptador para M-Pesa, e-Mola e banco: trocar a implementação não afeta o resto do sistema. */
public interface PagamentoGateway { boolean cobrar(String metodo, String numero, double valor); }
