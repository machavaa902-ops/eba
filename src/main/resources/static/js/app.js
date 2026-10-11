/* SGA-EBA – frontend (HTML + Bootstrap 5 + JS). Comunica com a API REST em /api. */
const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const MT = v => Number(v).toLocaleString('pt-PT', {minimumFractionDigits: 2, maximumFractionDigits: 2}) + ' MT';
const NIVEIS = ['6.ª Classe', '7.ª Classe', '8.ª Classe', '9.ª Classe'];
const MESES = ['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'];
const PERFIS = {ENCARREGADO: 'Aluno/Encarregado', SECRETARIA: 'Secretária', PROFESSOR: 'Professor', DIRETOR: 'Diretor/Administrador'};
let S = JSON.parse(sessionStorage.getItem('sga') || 'null');

async function api(p, o = {}) {
  const r = await fetch('/api' + p, {method: o.m || 'GET', headers: {'Content-Type': 'application/json', ...(S ? {Authorization: 'Bearer ' + S.token} : {})}, body: o.b ? JSON.stringify(o.b) : undefined});
  if (r.status === 401 && S) { sair(); throw new Error('Sessão expirada.'); }
  if (r.status === 403) throw new Error('Acesso negado.');
  const t = await r.text(); let d = {}; try { d = t ? JSON.parse(t) : {}; } catch { }
  if (!r.ok) throw new Error(d.erro || 'Erro ' + r.status);
  return d;
}
function toast(msg, ok = true) { const t = $('#toast'); t.className = 'toast align-items-center border-0 text-bg-' + (ok ? 'success' : 'danger'); $('#tb').textContent = msg; bootstrap.Toast.getOrCreateInstance(t).show(); }
const modal = (t, b, f = '') => { $('#mt').textContent = t; $('#mb').innerHTML = b; $('#mf').innerHTML = f; const m = bootstrap.Modal.getOrCreateInstance('#modal'); m.show(); return m; };
const view = h => $('#view').innerHTML = h;
const guard = fn => async (...a) => { try { return await fn(...a); } catch (e) { toast(e.message, false); } };
const bdg = e => { const [c, t] = {PAGA: ['success', 'Paga'], ATRASADA: ['danger', 'Em atraso'], PENDENTE: ['warning', 'Pendente'], ATIVO: ['success', 'Ativo'], REPROVADO_FALTAS: ['danger', 'Reprovado por faltas']}[e] || ['secondary', e]; return `<span class="badge text-bg-${c}">${t}</span>`; };
const head = (t, sub = '') => `<h4 class="text-eba mb-0">${t}</h4><p class="text-muted small">${sub}</p>`;
const kpi = (ic, v, l) => `<div class="col-6 col-xl-3"><div class="card kpi h-100"><div class="card-body d-flex gap-3 align-items-center"><i class="bi ${ic}"></i><div><div class="fs-5 fw-bold">${v}</div><small class="text-muted">${l}</small></div></div></div></div>`;
const tbl = (cols, rows) => `<div class="table-responsive"><table class="table table-sm align-middle"><thead class="table-light"><tr>${cols.map(c => `<th>${c}</th>`).join('')}</tr></thead><tbody>${rows.join('') || `<tr><td colspan="${cols.length}" class="text-muted text-center">Sem registos.</td></tr>`}</tbody></table></div>`;
const opts = (a, v, l, sel) => a.map(x => `<option value="${esc(v(x))}" ${v(x) == sel ? 'selected' : ''}>${esc(l(x))}</option>`).join('');

/* ---------- Autenticação (UC01) ---------- */
$('#aRec').onclick = e => { e.preventDefault(); $('#fLogin').classList.add('d-none'); $('#fRec').classList.remove('d-none'); };
$('#aVoltar').onclick = e => { e.preventDefault(); $('#fRec').classList.add('d-none'); $('#fLogin').classList.remove('d-none'); };
$('#fLogin').onsubmit = async e => {
  e.preventDefault(); $('#lErr').classList.add('d-none');
  try { S = await api('/auth/login', {m: 'POST', b: {email: $('#lEmail').value, senha: $('#lSenha').value}}); sessionStorage.setItem('sga', JSON.stringify(S)); entrar(); }
  catch (er) { $('#lErr').textContent = er.message; $('#lErr').classList.remove('d-none'); }
};
$('#fRec').onsubmit = async e => { e.preventDefault(); const r = await api('/auth/recuperar', {m: 'POST', b: {email: $('#rEmail').value}}); $('#rMsg').textContent = r.mensagem; $('#rMsg').classList.remove('d-none'); };
function sair() { if (S) fetch('/api/auth/logout', {method: 'POST', headers: {Authorization: 'Bearer ' + S.token}}); S = null; sessionStorage.removeItem('sga'); $('#app').classList.add('d-none'); $('#auth').classList.remove('d-none'); $('#fLogin').reset(); }
$('#bSair').onclick = sair;

/* ---------- Shell: menu por perfil (RD10) ---------- */
const MENUS = {
  ENCARREGADO: [['notas', 'bi-journal-text', 'Notas e Faltas', vNotas], ['inscricao', 'bi-person-plus', 'Inscrição online', vInscricao], ['pag', 'bi-credit-card', 'Pagamentos', vPagamentos], ['notif', 'bi-bell', 'Notificações', () => vNotif('/encarregado/notificacoes')]],
  SECRETARIA: [['alunos', 'bi-people', 'Alunos e Turmas', vAlunos], ['insc', 'bi-clipboard-check', 'Inscrições pendentes', vInsc], ['mens', 'bi-cash-coin', 'Mensalidades e Recibos', vMens], ['notif', 'bi-bell', 'Notificações', () => vNotif('/secretaria/notificacoes')]],
  PROFESSOR: [['notas', 'bi-pencil-square', 'Lançar notas', vLancar], ['faltas', 'bi-calendar-check', 'Registar faltas', vFaltas]],
  DIRETOR: [['prof', 'bi-person-badge', 'Professores', vProf], ['est', 'bi-bar-chart', 'Estatísticas e Relatórios', vEst]]
};
function entrar() {
  $('#auth').classList.add('d-none'); $('#app').classList.remove('d-none');
  $('#uNome').textContent = S.nome; $('#uPerfil').textContent = PERFIS[S.perfil]; $('#portal').textContent = 'Portal – ' + PERFIS[S.perfil];
  $('#menu').innerHTML = MENUS[S.perfil].map(m => `<a href="#" class="nav-link" data-id="${m[0]}"><i class="bi ${m[1]} me-2"></i>${m[2]}</a>`).join('');
  $$('#menu a').forEach(a => a.onclick = e => { e.preventDefault(); ir(a.dataset.id); });
  ir(MENUS[S.perfil][0][0]);
}
function ir(id) {
  $$('#menu a').forEach(a => a.classList.toggle('active', a.dataset.id === id));
  bootstrap.Offcanvas.getInstance('#side')?.hide();
  guard(MENUS[S.perfil].find(m => m[0] === id)[3])();
}

/* ---------- Recibo / boletim (impressão = guardar PDF) ---------- */
function recibo(m, aluno) {
  modal('Recibo ' + m.recibo, `<div class="text-center mb-3"><b class="text-eba fs-5">Centro Ebenezer Academy</b><div class="small text-muted">Recibo de mensalidade</div></div>
    <dl class="row mb-0"><dt class="col-5">Recibo nº</dt><dd class="col-7">${m.recibo}</dd><dt class="col-5">Aluno</dt><dd class="col-7">${esc(aluno)}</dd><dt class="col-5">Referência</dt><dd class="col-7">${m.mes} ${m.ano}</dd>
    <dt class="col-5">Valor</dt><dd class="col-7">${MT(m.valor)}</dd><dt class="col-5">Multa</dt><dd class="col-7">${MT(m.multa)}</dd><dt class="col-5">Total pago</dt><dd class="col-7 fw-bold">${MT(m.total)}</dd>
    <dt class="col-5">Data / método</dt><dd class="col-7">${m.pagoEm} · ${m.metodo}</dd></dl>`, `<button class="btn btn-eba" onclick="window.print()"><i class="bi bi-printer"></i> Imprimir / PDF</button>`);
}
function boletim(r) {
  modal('Boletim escolar – ' + r.nome, `<p class="small text-muted">${r.turma} · Média geral ${r.mediaGeral.toFixed(1)}</p>` + tbl(['Disciplina', '1.º T', '2.º T', '3.º T', 'Média', 'Situação'], r.boletim.map(b => `<tr><td>${esc(b.disciplina)}</td><td>${b.t1 ?? '–'}</td><td>${b.t2 ?? '–'}</td><td>${b.t3 ?? '–'}</td><td><b>${b.media.toFixed(1)}</b></td><td>${b.aprovado ? '<span class="badge text-bg-success">Aprovado</span>' : '<span class="badge text-bg-danger">Não aprovado</span>'}</td></tr>`)), `<button class="btn btn-eba" onclick="window.print()"><i class="bi bi-printer"></i> Imprimir / PDF</button>`);
}

/* ================= ENCARREGADO ================= */
async function seletorEducando(render) {
  const eds = await api('/encarregado/educandos');
  if (!eds.length) return view('<div class="alert alert-info">Ainda não tem educandos registados.</div>');
  const w = $('#sel'); w.innerHTML = `<select id="ed" class="form-select">${opts(eds, e => e.id, e => `${e.nome} · ${e.turma}`)}</select>`;
  const go = guard(() => render(eds.find(e => e.id == $('#ed').value))); $('#ed').onchange = go; go();
}
async function vNotas() {
  view(`<div class="d-flex flex-wrap justify-content-between gap-2">${head('Notas e Faltas', 'Consulta em tempo real')}<div id="sel" class="mb-2" style="min-width:260px"></div></div><div id="res"></div>`);
  seletorEducando(async ed => {
    const r = await api(`/encarregado/alunos/${ed.id}/resumo`); const pior = r.faltas.reduce((a, f) => Math.max(a, f.faltas), 0);
    $('#res').innerHTML = `<div class="row g-3 mb-3">${kpi('bi-award', r.mediaGeral.toFixed(1), 'Média geral (0–20)')}${kpi('bi-clipboard', pior + ' de ' + (r.faltas[0]?.aulas ?? 40), 'Faltas (pior disciplina) no ' + r.trimestre + '.º T')}<div class="col-12 col-xl-6"><div class="card kpi h-100"><div class="card-body d-flex justify-content-between align-items-center">${bdg(r.estado)}<button class="btn btn-sm btn-outline-success" id="bBol"><i class="bi bi-file-text"></i> Ver boletim escolar</button></div></div></div></div>
    <div class="row g-3"><div class="col-lg-8"><div class="card"><div class="card-body"><h6>Notas por disciplina</h6>${tbl(['Disciplina', '1.º T', '2.º T', '3.º T', 'Média', 'Situação'], r.boletim.map(b => `<tr><td>${esc(b.disciplina)}</td><td>${b.t1 ?? '–'}</td><td>${b.t2 ?? '–'}</td><td>${b.t3 ?? '–'}</td><td class="${b.aprovado ? '' : 'text-danger'} fw-bold">${b.media.toFixed(1)}</td><td>${b.aprovado ? '<span class="badge text-bg-success">Aprovado</span>' : '<span class="badge text-bg-danger">Não aprovado</span>'}</td></tr>`))}</div></div></div>
    <div class="col-lg-4"><div class="card"><div class="card-body"><h6>Faltas (limite 25% – RD05)</h6>${r.faltas.map(f => `<div class="small mt-2">${esc(f.disciplina)} <span class="float-end">${f.faltas}/${f.limite} permitidas</span></div><div class="progress"><div class="progress-bar ${f.acima ? 'bg-danger' : 'bg-success'}" style="width:${Math.min(100, f.faltas / f.aulas * 400)}%"></div></div>`).join('') || '<p class="text-muted small">Sem faltas registadas.</p>'}</div></div></div></div>`;
    $('#bBol').onclick = () => boletim(r);
  });
}
async function vInscricao() {
  view(`${head('Inscrição online de novo aluno', 'A secretária validará a inscrição.')}<div class="row g-3"><div class="col-lg-6"><form id="f" class="card card-body vstack gap-2">
    <input name="nome" class="form-control" placeholder="Nome completo" required><div class="row g-2"><div class="col"><input name="nascimento" type="date" class="form-control" required></div><div class="col"><select name="genero" class="form-select"><option>Masculino</option><option>Feminino</option></select></div></div>
    <select name="nivel" id="niv" class="form-select" required><option value="">Nível / classe pretendido…</option>${NIVEIS.map(n => `<option>${n}</option>`).join('')}</select>
    <div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba">Confirmar inscrição</button></form></div>
    <div class="col-lg-6"><div class="card card-body"><h6>Turmas compatíveis</h6><div id="tur" class="vstack gap-2 text-muted small">Escolha o nível.</div></div></div></div>`);
  $('#niv').onchange = guard(async () => {
    const t = await api('/encarregado/turmas?nivel=' + encodeURIComponent($('#niv').value));
    $('#tur').innerHTML = t.map(x => `<label class="border rounded p-2 d-flex justify-content-between ${x.vagas ? '' : 'opacity-50'}"><span><input type="radio" name="turmaId" value="${x.id}" ${x.vagas ? '' : 'disabled'} class="form-check-input me-2">${x.label}<br><small class="text-muted">${x.ocupacao}/${x.capacidade} lugares</small></span>${x.vagas ? `<span class="badge text-bg-success align-self-center">${x.vagas} vagas</span>` : '<span class="badge text-bg-danger align-self-center">Sem vagas</span>'}</label>`).join('') || 'Sem turmas para este nível.';
  });
  $('#f').onsubmit = async e => {
    e.preventDefault(); const b = Object.fromEntries(new FormData(e.target)); b.turmaId = document.querySelector('[name=turmaId]:checked')?.value; $('#err').classList.add('d-none');
    try { await api('/encarregado/inscricoes', {m: 'POST', b}); toast('Inscrição registada: pendente de validação.'); e.target.reset(); $('#tur').textContent = 'Escolha o nível.'; }
    catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); }
  };
}
async function vPagamentos() {
  view(`<div class="d-flex flex-wrap justify-content-between gap-2">${head('Extrato de pagamentos', 'Mensalidade vence no dia 8 · multa de 10% após o prazo')}<div id="sel" class="mb-2" style="min-width:260px"></div></div><div id="res"></div>`);
  seletorEducando(async ed => {
    const l = await api(`/encarregado/alunos/${ed.id}/pagamentos`), div = l.filter(x => x.estado !== 'PAGA').reduce((a, x) => a + x.total, 0);
    $('#res').innerHTML = `<div class="row g-3 mb-3">${kpi('bi-check2-circle', l.filter(x => x.estado === 'PAGA').length + ' de ' + l.length, 'Mensalidades pagas')}${kpi('bi-exclamation-triangle', MT(div), 'Em dívida (inclui multa)')}</div><div class="card"><div class="card-body">${tbl(['Mês', 'Vencimento', 'Valor', 'Multa', 'Total', 'Estado', 'Pago em', 'Recibo / Ação'], l.map(m => `<tr><td>${m.mes}</td><td>${m.vencimento}</td><td>${MT(m.valor)}</td><td>${m.multa ? MT(m.multa) : '—'}</td><td class="fw-bold">${MT(m.total)}</td><td>${bdg(m.estado)}</td><td>${m.pagoEm ?? '—'}</td><td>${m.estado === 'PAGA' ? `<button class="btn btn-sm btn-outline-success" data-r="${m.id}"><i class="bi bi-download"></i> ${m.recibo}</button>` : `<button class="btn btn-sm btn-eba" data-p="${m.id}">Pagar agora</button>`}</td></tr>`))}</div></div>`;
    $$('[data-r]').forEach(b => b.onclick = () => recibo(l.find(x => x.id == b.dataset.r), ed.nome));
    $$('[data-p]').forEach(b => b.onclick = () => pagar(l.find(x => x.id == b.dataset.p), ed));
  });
}
function pagar(m, ed) {
  const mod = modal(`Efetuar pagamento – ${m.mes} ${m.ano}`, `<dl class="row"><dt class="col-5">Aluno</dt><dd class="col-7">${esc(ed.nome)}</dd><dt class="col-5">Mensalidade</dt><dd class="col-7">${MT(m.valor)}</dd><dt class="col-5">Multa por atraso</dt><dd class="col-7 text-danger">${MT(m.multa)}</dd><dt class="col-5">Total a pagar</dt><dd class="col-7 fw-bold">${MT(m.total)}</dd></dl>
    ${[['MPESA', 'M-Pesa (Vodacom)'], ['EMOLA', 'e-Mola (Movitel)'], ['TRANSFERENCIA', 'Transferência bancária']].map((x, i) => `<label class="border rounded p-2 d-block mb-2"><input type="radio" name="met" value="${x[0]}" class="form-check-input me-2" ${i ? '' : 'checked'}>${x[1]}</label>`).join('')}
    <input id="num" class="form-control" placeholder="Número de telemóvel (ex.: 84 512 3398)"><div id="perr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba w-100" id="pg"><i class="bi bi-lock"></i> Pagar ${MT(m.total)}</button>`);
  $('#pg').onclick = async () => {
    try { const r = await api('/encarregado/pagamentos', {m: 'POST', b: {mensalidadeId: m.id, metodo: document.querySelector('[name=met]:checked').value, numero: $('#num').value}}); mod.hide(); toast('Pagamento confirmado. Recibo ' + r.recibo); vPagamentos(); }
    catch (e) { $('#perr').textContent = e.message; }
  };
}
async function vNotif(url) {
  const l = await api(url);
  view(head('Notificações') + `<div class="list-group">${l.map(n => `<div class="list-group-item"><i class="bi bi-bell text-eba me-2"></i>${esc(n.mensagem)} <small class="text-muted float-end">${n.data}</small></div>`).join('') || '<div class="text-muted">Sem notificações.</div>'}</div>`);
}

/* ================= SECRETÁRIA ================= */
async function vAlunos() {
  const [tur, al] = await Promise.all([api('/secretaria/turmas'), api('/secretaria/alunos')]);
  view(`<div class="d-flex justify-content-between">${head('Alunos e Turmas', 'Registo de alunos, encarregados e turmas')}<button class="btn btn-outline-success btn-sm align-self-start" data-bs-toggle="collapse" data-bs-target="#nt"><i class="bi bi-plus"></i> Nova turma</button></div>
  <form id="ft" class="collapse card card-body mb-3"><div class="row g-2"><div class="col-6 col-md-2"><input name="codigo" class="form-control" placeholder="Código (A)" required></div><div class="col-6 col-md-3"><select name="nivel" class="form-select">${NIVEIS.map(n => `<option>${n}</option>`).join('')}</select></div><div class="col-6 col-md-2"><input name="capacidade" type="number" min="1" value="30" class="form-control"></div><div class="col-6 col-md-3"><select name="turno" class="form-select"><option>Manhã</option><option>Tarde</option></select></div><div class="col-12 col-md-2"><button class="btn btn-eba w-100">Criar</button></div></div></form>
  <div class="row g-3 mb-3"><div class="col-lg-7"><form id="fa" class="card card-body"><h6>Novo aluno</h6><div class="row g-2">
    <div class="col-12 col-md-6"><input name="nome" class="form-control" placeholder="Nome completo" required></div><div class="col-6 col-md-3"><input name="nascimento" type="date" class="form-control"></div><div class="col-6 col-md-3"><select name="genero" class="form-select"><option>Masculino</option><option>Feminino</option></select></div>
    <div class="col-6"><select name="nivel" id="nv" class="form-select" required><option value="">Nível / classe</option>${NIVEIS.map(n => `<option>${n}</option>`).join('')}</select></div><div class="col-6"><select name="turmaId" id="tn" class="form-select" required><option value="">Turma</option></select></div>
    <div class="col-12"><small class="fw-semibold">Encarregado de educação (obrigatório)</small></div><div class="col-md-4"><input name="enome" class="form-control" placeholder="Nome"></div><div class="col-md-4"><input name="econtacto" class="form-control" placeholder="Contacto"></div><div class="col-md-4"><input name="eemail" type="email" class="form-control" placeholder="Email"></div>
    <div class="col-12"><div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba">Guardar aluno</button></div></div></form></div>
  <div class="col-lg-5"><div class="card card-body"><h6>Ocupação das turmas</h6>${tur.map(t => `<div class="small mt-2">${t.label}<span class="float-end">${t.ocupacao}/${t.capacidade}${t.vagas ? '' : ' · cheia'}</span></div><div class="progress"><div class="progress-bar ${t.vagas ? (t.ocupacao / t.capacidade > .85 ? 'bg-warning' : 'bg-success') : 'bg-danger'}" style="width:${t.ocupacao / t.capacidade * 100}%"></div></div>`).join('')}</div></div></div>
  <div class="card card-body">${tbl(['Aluno', 'Turma', 'Encarregado', 'Estado'], al.map(a => `<tr><td>${esc(a.nome)}</td><td>${esc(a.turma)}</td><td>${esc(a.encarregado)}</td><td>${a.estado === 'PENDENTE' ? '<span class="badge text-bg-warning">Pendente</span>' : bdg(a.estado)}</td></tr>`))}</div>`);
  $('#nv').onchange = () => $('#tn').innerHTML = '<option value="">Turma</option>' + tur.filter(t => t.nivel === $('#nv').value).map(t => `<option value="${t.id}" ${t.vagas ? '' : 'disabled'}>${t.label} (${t.vagas} vagas)</option>`).join('');
  $('#ft').onsubmit = guard(async e => { e.preventDefault(); await api('/secretaria/turmas', {m: 'POST', b: Object.fromEntries(new FormData(e.target))}); toast('Turma criada.'); vAlunos(); });
  $('#fa').onsubmit = async e => {
    e.preventDefault(); const f = Object.fromEntries(new FormData(e.target)); $('#err').classList.add('d-none');
    const b = {nome: f.nome, nascimento: f.nascimento || null, genero: f.genero, nivel: f.nivel, turmaId: f.turmaId, encarregado: {nome: f.enome, contacto: f.econtacto, email: f.eemail}};
    try { await api('/secretaria/alunos', {m: 'POST', b}); toast('Aluno registado (palavra-passe inicial do encarregado: 1234).'); vAlunos(); }
    catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); }
  };
}
async function vInsc() {
  const l = await api('/secretaria/alunos?estado=PENDENTE');
  view(head('Inscrições pendentes', 'Inscrições online a aguardar validação') + `<div class="card card-body">${tbl(['Aluno', 'Turma', 'Encarregado', ''], l.map(a => `<tr><td>${esc(a.nome)}</td><td>${esc(a.turma)}</td><td>${esc(a.encarregado)}</td><td><button class="btn btn-sm btn-eba" data-v="${a.id}">Validar</button></td></tr>`))}</div>`);
  $$('[data-v]').forEach(b => b.onclick = guard(async () => { await api(`/secretaria/inscricoes/${b.dataset.v}/validar`, {m: 'POST'}); toast('Inscrição validada.'); vInsc(); }));
}
async function vMens() {
  const hoje = new Date(); let mes = window._mes ?? hoje.getMonth() + 1, ano = window._ano ?? hoje.getFullYear();
  const r = await api(`/secretaria/mensalidades?mes=${mes}&ano=${ano}`);
  view(`<div class="d-flex flex-wrap justify-content-between gap-2">${head('Mensalidades e recibos', 'Prazo de pagamento: dia 8 de cada mês')}<div class="d-flex gap-2 align-self-start"><select id="mm" class="form-select form-select-sm">${MESES.map((m, i) => `<option value="${i + 1}" ${i + 1 == mes ? 'selected' : ''}>${m}</option>`).join('')}</select><input id="aa" type="number" class="form-control form-control-sm" style="width:90px" value="${ano}"><button id="gen" class="btn btn-eba btn-sm text-nowrap"><i class="bi bi-plus"></i> Gerar do mês</button></div></div>
  <div class="row g-3 mb-3">${kpi('bi-wallet2', MT(r.recebido), 'Recebido no mês')}${kpi('bi-check2', r.pagas, 'Mensalidades pagas')}${kpi('bi-calendar', r.pendentes, 'Pendentes')}${kpi('bi-exclamation-triangle', r.atrasadas, 'Em atraso (com multa)')}</div>
  <div class="card card-body">${tbl(['Aluno', 'Turma', 'Valor', 'Multa', 'Estado', 'Ação / Recibo'], r.linhas.map(m => `<tr><td>${esc(m.aluno)}</td><td>${esc(m.turma)}</td><td>${MT(m.valor)}</td><td>${m.multa ? MT(m.multa) : '—'}</td><td>${bdg(m.estado)}</td><td>${m.estado === 'PAGA' ? `<button class="btn btn-sm btn-outline-success" data-r="${m.id}"><i class="bi bi-download"></i> ${m.recibo}</button>` : `<button class="btn btn-sm btn-eba" data-p="${m.id}"><i class="bi bi-check"></i> Registar pagamento</button>`}</td></tr>`))}</div>`);
  const mudar = () => { window._mes = +$('#mm').value; window._ano = +$('#aa').value; guard(vMens)(); };
  $('#mm').onchange = $('#aa').onchange = mudar;
  $('#gen').onclick = guard(async () => { const g = await api('/secretaria/mensalidades/gerar', {m: 'POST', b: {mes: +$('#mm').value, ano: +$('#aa').value}}); toast(g.geradas + ' mensalidades geradas.'); vMens(); });
  $$('[data-r]').forEach(b => b.onclick = () => { const m = r.linhas.find(x => x.id == b.dataset.r); recibo(m, m.aluno); });
  $$('[data-p]').forEach(b => b.onclick = guard(async () => { const m = await api(`/secretaria/mensalidades/${b.dataset.p}/pagar`, {m: 'POST'}); toast('Pagamento registado. Recibo ' + m.recibo); vMens(); }));
}

/* ================= PROFESSOR ================= */
async function turmaSel(extra, onload, bind) {
  const t = await api('/professor/turmas');
  view(`${head('Disciplina: ' + esc(t.disciplina), 'Turmas e alunos')}<div class="row g-2 mb-3"><div class="col-md-4"><select id="tu" class="form-select">${opts(t.turmas, x => x.id, x => x.label)}</select></div>${extra}</div><div id="res"></div>`);
  $$('#view select,#view input').forEach(i => i.onchange = guard(onload)); bind?.(); guard(onload)();
}
function vLancar() {
  turmaSel(`<div class="col-md-3"><select id="tri" class="form-select"><option value="1">1.º Trimestre</option><option value="2">2.º Trimestre</option><option value="3" selected>3.º Trimestre</option></select></div><div class="col-md-3"><button id="gu" class="btn btn-eba w-100"><i class="bi bi-check"></i> Guardar notas</button></div>`, async () => {
    const tri = +$('#tri').value, al = await api(`/professor/turmas/${$('#tu').value}/alunos?trimestre=${tri}`);
    $('#res').innerHTML = `<div class="card card-body">${tbl(['Aluno', '1.º T', '2.º T', '3.º T', 'Média', 'Situação'], al.map(a => `<tr data-id="${a.id}"><td>${esc(a.nome)}</td>${[1, 2, 3].map(i => i === tri ? `<td><input class="form-control form-control-sm nota-in" type="number" step="0.1" value="${a['t' + i] ?? ''}"></td>` : `<td data-t="${i}">${a['t' + i] ?? '–'}</td>`).join('')}<td class="med fw-bold"></td><td class="sit"></td></tr>`))}<div id="verr" class="alert alert-danger py-2 d-none"></div></div>`;
    const calc = tr => {
      const inp = tr.querySelector('input'), v = inp.value === '' ? null : parseFloat(inp.value), ok = v === null || (v >= 0 && v <= 20); inp.classList.toggle('is-invalid', !ok);
      const nums = [...tr.querySelectorAll('[data-t]')].map(c => parseFloat(c.textContent)).filter(x => !isNaN(x)); if (v !== null && ok) nums.push(v);     // RD04
      const m = nums.length ? nums.reduce((a, b) => a + b) / nums.length : null;
      tr.querySelector('.med').textContent = ok && m !== null ? m.toFixed(1) : '—';
      tr.querySelector('.sit').innerHTML = !ok ? '<span class="badge text-bg-danger">Valor inválido</span>' : m === null ? '' : m >= 10 ? '<span class="badge text-bg-success">Aprovado</span>' : '<span class="badge text-bg-danger">Não aprovado</span>';   // RD02
      return ok;
    };
    $$('#res tbody tr[data-id]').forEach(tr => { calc(tr); tr.querySelector('input').oninput = () => { calc(tr); const bad = $$('#res .is-invalid').length; $('#verr').classList.toggle('d-none', !bad); $('#verr').textContent = 'Existem notas fora do intervalo permitido (0 a 20 valores). Corrija para poder guardar.'; }; });
    $('#gu').onclick = guard(async () => {
      if ($$('#res .is-invalid').length) return toast('Corrija as notas inválidas (0–20).', false);
      let n = 0; for (const tr of $$('#res tbody tr[data-id]')) { const v = tr.querySelector('input').value; if (v !== '') { await api('/professor/notas', {m: 'POST', b: {alunoId: tr.dataset.id, trimestre: tri, valor: v}}); n++; } }
      toast(n + ' notas guardadas.');
    });
  });
}
function vFaltas() {
  const hoje = new Date().toISOString().slice(0, 10);
  turmaSel(`<div class="col-md-3"><input id="dt" type="date" class="form-control" value="${hoje}"></div><div class="col-md-3"><button id="gu" class="btn btn-eba w-100"><i class="bi bi-check"></i> Guardar faltas</button></div>`, async () => {
    const mth = new Date($('#dt').value).getMonth() + 1, tri = mth <= 4 ? 1 : mth <= 8 ? 2 : 3, al = await api(`/professor/turmas/${$('#tu').value}/alunos?trimestre=${tri}`);
    $('#res').innerHTML = `<div class="card card-body"><p class="small text-muted">Aulas previstas no trimestre: 40 · Limite de faltas: 25% (10 aulas)</p>${tbl(['Aluno', 'Presença na aula', 'Faltas no trimestre', 'Situação'], al.map(a => `<tr data-id="${a.id}"><td>${esc(a.nome)}</td><td><div class="btn-group btn-group-sm">${[['PRESENTE', 'Presente', 'success'], ['FALTA', 'Falta', 'danger'], ['JUSTIFICADA', 'Justificada', 'warning']].map((o, i) => `<input type="radio" class="btn-check" name="p${a.id}" id="p${a.id}${i}" value="${o[0]}" ${i ? '' : 'checked'}><label class="btn btn-outline-${o[2]}" for="p${a.id}${i}">${o[1]}</label>`).join('')}</div></td><td class="${a.faltas > a.limite ? 'text-danger fw-bold' : ''}">${a.faltas} / ${a.aulas}</td><td>${a.faltas > a.limite ? '<span class="badge text-bg-danger">Acima do limite</span>' : '<span class="badge text-bg-success">Dentro do limite</span>'}</td></tr>`))}</div>`;
  }, () => $('#gu').onclick = guard(async () => {
    const itens = $$('#res tbody tr[data-id]').map(tr => ({alunoId: tr.dataset.id, tipo: tr.querySelector('input:checked').value}));
    const r = await api('/professor/faltas', {m: 'POST', b: {data: $('#dt').value, itens}}); r.alertas.forEach(a => toast(a + ' Encarregado notificado.', false)); toast('Faltas registadas.'); $('#tu').onchange();
  }));
}

/* ================= DIRETOR ================= */
async function vProf() {
  const l = await api('/diretor/professores'); let edit = null;
  view(`${head('Gestão de professores', l.length + ' professores registados')}<div class="row g-3"><div class="col-lg-8"><div class="card card-body">${tbl(['Professor', 'Especialidade', 'Contratação', 'Contacto', ''], l.map(p => `<tr><td>${esc(p.nome)}<br><small class="text-muted">${esc(p.email)}</small></td><td>${esc(p.especialidade)}</td><td>${p.dataContratacao}</td><td>${esc(p.contacto)}</td><td><button class="btn btn-sm btn-outline-success" data-e="${p.id}"><i class="bi bi-pencil"></i></button></td></tr>`))}</div></div>
  <div class="col-lg-4"><form id="f" class="card card-body vstack gap-2"><h6 id="ft">Registar novo professor</h6><input name="nome" class="form-control" placeholder="Nome completo"><input name="especialidade" class="form-control" placeholder="Especialidade (disciplina)"><input name="dataContratacao" type="date" class="form-control"><input name="contacto" class="form-control" placeholder="Contacto"><input name="email" type="email" class="form-control" placeholder="Email (utilizador)">
  <small class="text-muted">É criada a conta com perfil de professor (palavra-passe inicial 1234).</small><div id="err" class="alert alert-danger py-2 d-none"></div><div class="d-flex gap-2"><button type="button" class="btn btn-outline-secondary" onclick="vProf()">Cancelar</button><button class="btn btn-eba flex-grow-1">Guardar</button></div></form></div></div>`);
  $$('[data-e]').forEach(b => b.onclick = () => { edit = l.find(p => p.id == b.dataset.e); $('#ft').textContent = 'Editar professor'; for (const k of ['nome', 'especialidade', 'dataContratacao', 'contacto', 'email']) document.querySelector(`[name=${k}]`).value = edit[k] ?? ''; });
  $('#f').onsubmit = async e => {
    e.preventDefault(); $('#err').classList.add('d-none');
    try { await api(edit ? '/diretor/professores/' + edit.id : '/diretor/professores', {m: edit ? 'PUT' : 'POST', b: Object.fromEntries(new FormData(e.target))}); toast('Professor guardado.'); vProf(); }
    catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); }
  };
}
async function exportar(tipo) {
  const r = await fetch('/api/diretor/relatorio?tipo=' + tipo, {headers: {Authorization: 'Bearer ' + S.token}}); if (!r.ok) return toast('Sem dados para exportar.', false);
  const a = document.createElement('a'); a.href = URL.createObjectURL(await r.blob()); a.download = `relatorio-${tipo}.csv`; a.click();
}
async function vEst() {
  const e = await api('/diretor/estatisticas'), mx = Math.max(...e.receitasMensais, 1), cur = new Date().getMonth();
  view(`<div class="d-flex flex-wrap justify-content-between gap-2">${head('Estatísticas e relatórios', 'Indicadores gerais do centro')}<div class="align-self-start d-flex gap-2"><button class="btn btn-outline-success btn-sm" onclick="exportar('financeiro')"><i class="bi bi-download"></i> Financeiro (Excel/CSV)</button><button class="btn btn-eba btn-sm" onclick="exportar('academico')"><i class="bi bi-download"></i> Académico (Excel/CSV)</button></div></div>
  <div class="row g-3 mb-3">${kpi('bi-people', e.alunos, 'Alunos matriculados')}${kpi('bi-grid', e.turmas, 'Turmas ativas')}${kpi('bi-graph-up', MT(e.receitaMes), 'Receita do mês')}${kpi('bi-exclamation-triangle', MT(e.dividas), 'Dívidas pendentes')}</div>
  <div class="row g-3"><div class="col-lg-7"><div class="card card-body"><h6>Receitas mensais (MT)</h6><div class="bars">${e.receitasMensais.map((v, i) => `<div class="${i === cur ? 'cur' : ''}" style="height:${v / mx * 100}%" title="${MESES[i]}: ${MT(v)}"></div>`).join('')}</div><div class="bar-lbl mt-1">${MESES.map(m => `<span>${m.slice(0, 3)}</span>`).join('')}</div></div></div>
  <div class="col-lg-5"><div class="card card-body"><h6>Melhores alunos</h6>${e.melhoresAlunos.map((a, i) => `<div class="d-flex justify-content-between border-bottom py-2"><span><span class="badge text-bg-success me-2">${i + 1}</span>${esc(a.nome)}<br><small class="text-muted">${esc(a.turma)}</small></span><b class="text-eba">${a.media.toFixed(1)}</b></div>`).join('') || '<p class="text-muted small">Sem notas lançadas.</p>'}</div></div></div>`);
}

if (S) entrar();
