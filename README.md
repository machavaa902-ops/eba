# SGA-EBA – Sistema de Gestão Académica (Centro Ebenezer Academy)

Backend: Spring Boot 4 / Java 21 / JPA / MySQL (camadas Controller → Service → Repository, RD01–RD10).
Frontend: HTML + CSS + JS + Bootstrap 5 (responsivo), servido em `src/main/resources/static`.

## Executar
1. MySQL a correr em `localhost:3306` (a base `eba` é criada automaticamente; ajuste utilizador/senha em `application.properties`).
2. `./mvnw spring-boot:run`  →  abrir http://localhost:8080
3. Na primeira execução são criados dados de demonstração. Todas as contas têm a palavra-passe `1234`:

| Perfil | Email |
|---|---|
| Aluno/Encarregado | encarregado@eba.co.mz (educandos: Ana, Nélio) · alberto@eba.co.mz |
| Secretária | secretaria@eba.co.mz |
| Professor | professor@eba.co.mz (Matemática) · luisa@eba.co.mz (Português) |
| Diretor | diretor@eba.co.mz |

## API (`Authorization: Bearer <token>`)
- `POST /api/auth/login | /recuperar | /logout`
- `/api/encarregado/**` – educandos, resumo (notas/faltas), pagamentos, pagar, turmas, inscrições, notificações
- `/api/secretaria/**` – turmas, alunos, inscrições/validar, mensalidades (listar/gerar/pagar)
- `/api/professor/**` – turmas, alunos da turma, notas, faltas
- `/api/diretor/**` – professores (CRUD), estatísticas, relatório CSV

## Regras implementadas
RD01 nota 0–20 · RD02 aprovado ≥ 10 · RD03 encarregado obrigatório · RD04 média dos trimestres · RD05 >25% de 40 aulas ⇒ reprovado por faltas + notificação · RD06 multa de 10% após o dia 8 · RD07 capacidade da turma · RD08 nível compatível · RD09 recibo após pagamento · RD10 acesso por perfil.

## Notas / limites desta versão
- Gateways M-Pesa/e-Mola/banco: **simulados** (`GatewaySimulado`; recusa números terminados em 0000). Para integrar, implementar `PagamentoGateway`.
- Email/SMS de recuperação: simulado (registado no log). Exportação em CSV (abre no Excel); PDF via "Imprimir" nos recibos/boletim.
- Sessões em memória; palavras-passe com SHA-256 + sal (em produção usar BCrypt/Argon2 e Spring Security).
