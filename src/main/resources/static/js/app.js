/* SGA-EBA – frontend (HTML + Bootstrap 5 + JS). Comunica com a API REST em /api. */
const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const MT = v => Number(v).toLocaleString('pt-PT', {minimumFractionDigits: 2, maximumFractionDigits: 2}) + ' MT';
let NIVEIS = [];   // níveis cadastrados pelo administrador (carregados da API)
async function carregarNiveis() { NIVEIS = (await api('/comum/niveis')).map(n => n.nome); }
const MESES = ['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'];
const PERFIS = {ENCARREGADO: 'Aluno/Encarregado', SECRETARIA: 'Secretária', PROFESSOR: 'Professor', DIRETOR: 'Diretor', ADMIN: 'Administrador'};
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
async function upload(p, fd) {
  const r = await fetch('/api' + p, {method: 'POST', headers: {Authorization: 'Bearer ' + S.token}, body: fd});
  const t = await r.text(); let d = {}; try { d = t ? JSON.parse(t) : {}; } catch { }
  if (!r.ok) throw new Error(d.erro || 'Erro ' + r.status); return d;
}
async function baixar(url, nome) {
  const r = await fetch('/api' + url, {headers: {Authorization: 'Bearer ' + S.token}}); if (!r.ok) return toast('Não foi possível descarregar o ficheiro.', false);
  const a = document.createElement('a'); a.href = URL.createObjectURL(await r.blob()); a.download = nome; a.click();
}
const kb = n => n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB';
const icone = n => /pdf$/i.test(n) ? 'bi-file-earmark-pdf text-danger' : /docx?$/i.test(n) ? 'bi-file-earmark-word text-primary' : /(png|jpe?g)$/i.test(n) ? 'bi-file-earmark-image text-success' : 'bi-file-earmark';
const bdg = e => { const [c, t] = {PAGA: ['success', 'Paga'], ATRASADA: ['danger', 'Em atraso'], PENDENTE: ['warning', 'Pendente'], ATIVO: ['success', 'Ativo'], REPROVADO_FALTAS: ['danger', 'Reprovado por faltas']}[e] || ['secondary', e]; return `<span class="badge text-bg-${c}">${t}</span>`; };
const head = (t, sub = '') => `<h4 class="text-eba mb-0">${t}</h4><p class="text-muted small">${sub}</p>`;
const kpi = (ic, v, l) => `<div class="col-6 col-xl-3"><div class="card kpi h-100"><div class="card-body d-flex gap-3 align-items-center"><i class="bi ${ic}"></i><div><div class="fs-5 fw-bold">${v}</div><small class="text-muted">${l}</small></div></div></div></div>`;
const tbl = (cols, rows) => `<div class="table-responsive"><table class="table table-sm align-middle"><thead class="table-light"><tr>${cols.map(c => `<th>${c}</th>`).join('')}</tr></thead><tbody>${rows.join('') || `<tr><td colspan="${cols.length}" class="text-muted text-center">Sem registos.</td></tr>`}</tbody></table></div>`;
const opts = (a, v, l, sel) => a.map(x => `<option value="${esc(v(x))}" ${v(x) == sel ? 'selected' : ''}>${esc(l(x))}</option>`).join('');

/* ---------- Páginas públicas: home, login, recuperar, 404 ---------- */
const root = () => $('#root');
const pagina = (html, t) => { root().dataset.shell = ''; root().innerHTML = html; document.title = t + ' · Ebenezer Academy'; window.scrollTo(0, 0); };
const MARCA = '<img src="/img/logo-mark.png" alt="EBA" height="38"><span class="brand-word d-none d-sm-inline">Ebenezer <b>Academy</b></span>';

function pagHome() {
  const ano = new Date().getFullYear();
  pagina(`<div class="container"><header class="home-nav"><a href="/" data-link class="d-flex align-items-center gap-2 text-decoration-none">${MARCA}</a>
    <a href="${S ? inicio() : '/login'}" data-link class="btn btn-eba px-4">${S ? 'Ir para o portal' : 'Entrar'}</a></header>
    <section class="hero row align-items-center g-4"><div class="col-lg-7"><h1>Gestão académica <span>simples</span>, do aluno ao encarregado.</h1>
      <p class="lead mt-3">Notas, testes, faltas, material de estudo e pagamentos do Centro Ebenezer Academy num só lugar, acessível de qualquer dispositivo.</p>
      <a href="${S ? inicio() : '/login'}" data-link class="btn btn-eba btn-lg px-4 mt-2">${S ? 'Ir para o portal' : 'Entrar no portal'} <i class="bi bi-arrow-right ms-1"></i></a></div>
      <div class="col-lg-5 text-center"><img src="/img/logo.png" alt="Ebenezer Academy" class="hero-logo"></div></section>
    <section class="row g-3">${[['bi-journal-text', 'Notas e testes', 'Acompanhe cada teste e a média de cada trimestre.'], ['bi-calendar-check', 'Faltas', 'Presenças por disciplina, com alerta automático.'], ['bi-folder2-open', 'Material de estudo', 'Fichas e documentos enviados pelos professores.'], ['bi-credit-card', 'Pagamentos', 'Mensalidades, recibos e dados para depósito.']].map(f => `<div class="col-6 col-lg-3"><div class="card feat"><i class="bi ${f[0]}"></i><h6>${f[1]}</h6><p class="small text-muted mb-0">${f[2]}</p></div></div>`).join('')}</section>
    <footer class="home-foot text-center">© ${ano} Centro Ebenezer Academy · Maputo, Moçambique</footer></div>`, 'Início');
}
function pagLogin() {
  pagina(`<div class="auth-wrap min-vh-100 d-flex align-items-center justify-content-center p-3"><div class="auth-card card w-100"><div class="card-body p-4 p-sm-5">
    <div class="text-center mb-4"><a href="/" data-link><img src="/img/logo.png" alt="Ebenezer Academy" class="auth-logo"></a></div>
    <form id="fLogin"><h5 class="text-center mb-4">Iniciar sessão</h5>
      <label class="form-label small">Email</label><input id="lEmail" type="email" class="form-control mb-3" required autocomplete="username" autofocus>
      <label class="form-label small">Palavra-passe</label><input id="lSenha" type="password" class="form-control mb-2" required autocomplete="current-password">
      <div class="text-end mb-3"><a href="/recuperar" data-link class="small">Recuperar palavra-passe</a></div>
      <div id="lErr" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba w-100 py-2" id="bEntrar">Entrar</button></form></div></div></div>`, 'Entrar');
  $('#fLogin').onsubmit = async e => {
    e.preventDefault(); $('#lErr').classList.add('d-none'); $('#bEntrar').disabled = true;
    try { S = await api('/auth/login', {m: 'POST', b: {email: $('#lEmail').value, senha: $('#lSenha').value}}); sessionStorage.setItem('sga', JSON.stringify(S)); try { await carregarNiveis(); } catch { } navigate(inicio(), true); }
    catch (er) { $('#lErr').textContent = er.message; $('#lErr').classList.remove('d-none'); $('#bEntrar').disabled = false; }
  };
}
function pagRecuperar() {
  pagina(`<div class="auth-wrap min-vh-100 d-flex align-items-center justify-content-center p-3"><div class="auth-card card w-100"><div class="card-body p-4 p-sm-5">
    <div class="text-center mb-4"><a href="/" data-link><img src="/img/logo.png" alt="Ebenezer Academy" class="auth-logo"></a></div>
    <form id="fRec"><h5 class="text-center mb-2">Recuperar palavra-passe</h5><p class="text-muted small text-center">Indique o email registado. Receberá um link de redefinição.</p>
      <input id="rEmail" type="email" class="form-control mb-3" placeholder="Email registado" required>
      <div id="rMsg" class="alert alert-success py-2 d-none"></div><button class="btn btn-eba w-100 py-2 mb-3">Enviar link de redefinição</button>
      <a href="/login" data-link class="small"><i class="bi bi-arrow-left"></i> Voltar ao início de sessão</a></form></div></div></div>`, 'Recuperar palavra-passe');
  $('#fRec').onsubmit = guard(async e => { e.preventDefault(); const r = await api('/auth/recuperar', {m: 'POST', b: {email: $('#rEmail').value}}); $('#rMsg').textContent = r.mensagem; $('#rMsg').classList.remove('d-none'); });
}
function pag404() {
  pagina(`<div class="auth-wrap min-vh-100 d-flex align-items-center justify-content-center p-3 text-center"><div><img src="/img/logo-mark.png" alt="EBA" height="70" class="mb-3"><h4>Página não encontrada</h4><p class="text-muted">O endereço que abriu não existe.</p><a href="${S ? inicio() : '/'}" data-link class="btn btn-eba">${S ? 'Ir para o portal' : 'Voltar ao início'}</a></div></div>`, 'Não encontrada');
}

/* ---------- Shell do portal (barra superior + menu por perfil, RD10) ---------- */
const MENUS = {
  ENCARREGADO: [['notas', 'bi-journal-text', 'Notas e Faltas', vNotas], ['inscricao', 'bi-person-plus', 'Inscrição online', vInscricao], ['mat', 'bi-folder2-open', 'Material académico', vMaterialEnc], ['pag', 'bi-credit-card', 'Pagamentos', vPagamentos], ['notif', 'bi-bell', 'Notificações', () => vNotif('/encarregado/notificacoes')]],
  SECRETARIA: [['alunos', 'bi-people', 'Alunos e Turmas', vAlunos], ['insc', 'bi-clipboard-check', 'Inscrições pendentes', vInsc], ['mens', 'bi-cash-coin', 'Mensalidades e Recibos', vMens], ['notif', 'bi-bell', 'Notificações', () => vNotif('/secretaria/notificacoes')]],
  PROFESSOR: [['notas', 'bi-pencil-square', 'Testes e notas', vLancar], ['mat', 'bi-cloud-upload', 'Material académico', vMaterialProf], ['faltas', 'bi-calendar-check', 'Registar faltas', vFaltas]],
  ADMIN: [['painel', 'bi-speedometer2', 'Painel', vAdPainel], ['niveis', 'bi-layers', 'Níveis', vAdNiveis], ['turmas', 'bi-grid', 'Turmas', vAdTurmas], ['disciplinas', 'bi-journal-bookmark', 'Disciplinas', vAdDisciplinas], ['users', 'bi-people', 'Utilizadores', vAdUsers], ['contas', 'bi-bank', 'Contas de pagamento', vAdContas]],
  DIRETOR: [['prof', 'bi-person-badge', 'Professores', vProf], ['est', 'bi-bar-chart', 'Estatísticas e Relatórios', vEst]]
};

const PORTAL = {ENCARREGADO: 'encarregado', SECRETARIA: 'secretaria', PROFESSOR: 'professor', DIRETOR: 'diretor', ADMIN: 'admin'};
const inicio = () => '/' + PORTAL[S.perfil] + '/' + MENUS[S.perfil][0][0];
const ir = id => navigate('/' + PORTAL[S.perfil] + '/' + id);
function montarShell() {
  if ($('#view') && root().dataset.shell === S.perfil) return;
  const b = '/' + PORTAL[S.perfil] + '/';
  root().dataset.shell = S.perfil;
  root().innerHTML = `<nav class="navbar topbar sticky-top px-3">
    <button class="btn btn-sm d-lg-none me-2" data-bs-toggle="offcanvas" data-bs-target="#side" aria-label="Menu"><i class="bi bi-list fs-4"></i></button>
    <a href="/" data-link class="d-flex align-items-center gap-2 text-decoration-none">${MARCA}</a>
    <div class="ms-auto d-flex align-items-center gap-3"><div class="text-end lh-sm d-none d-sm-block"><div class="fw-semibold" id="uNome"></div><small class="text-muted" id="uPerfil"></small></div>
      <button class="btn btn-sm btn-outline-secondary" id="bSair"><i class="bi bi-box-arrow-right"></i><span class="d-none d-md-inline"> Terminar sessão</span></button></div></nav>
    <div class="d-flex"><aside id="side" class="offcanvas-lg offcanvas-start sidebar" tabindex="-1">
      <div class="offcanvas-header d-lg-none"><b>Menu</b><button class="btn-close" data-bs-dismiss="offcanvas"></button></div>
      <div class="offcanvas-body flex-column p-2"><small class="portal-lbl px-2 mb-2" id="portal"></small><div id="menu" class="nav flex-column gap-1">${MENUS[S.perfil].map(m => `<a href="${b}${m[0]}" data-link class="nav-link" data-id="${m[0]}"><i class="bi ${m[1]} me-2"></i>${m[2]}</a>`).join('')}</div></div></aside>
    <main id="view" class="flex-grow-1 p-3 p-lg-4 overflow-x-hidden"></main></div>`;
  $('#uNome').textContent = S.nome; $('#uPerfil').textContent = PERFIS[S.perfil]; $('#portal').textContent = 'Portal ' + PERFIS[S.perfil];
  $('#bSair').onclick = sair;
}
function sair() { if (S) fetch('/api/auth/logout', {method: 'POST', headers: {Authorization: 'Bearer ' + S.token}}); S = null; sessionStorage.removeItem('sga'); navigate('/login', true); }

/* ---------- Roteador (History API: URLs limpas, voltar/avançar do browser, proteção por perfil) ---------- */
function navigate(p, replace) { if (p !== location.pathname) history[replace ? 'replaceState' : 'pushState']({}, '', p); return route(); }
document.addEventListener('click', e => { const a = e.target.closest('a[data-link]'); if (!a || e.ctrlKey || e.metaKey || e.shiftKey) return; e.preventDefault(); navigate(a.getAttribute('href')); });
window.addEventListener('popstate', () => route());
async function route() {
  const path = location.pathname.replace(/\/+$/, '') || '/', m = path.match(/^\/(admin|secretaria|professor|diretor|encarregado)(?:\/([\w-]+))?$/);
  if (path === '/') return pagHome();
  if (path === '/login' || path === '/recuperar') return S ? navigate(inicio(), true) : path === '/login' ? pagLogin() : pagRecuperar();
  if (!m) return pag404();
  if (!S) return navigate('/login', true);                               // sem sessão → login
  if (PORTAL[S.perfil] !== m[1]) return navigate(inicio(), true);        // portal de outro perfil → o seu portal
  const id = m[2] || MENUS[S.perfil][0][0], item = MENUS[S.perfil].find(x => x[0] === id);
  if (!item) return pag404();
  if (!m[2]) return navigate('/' + m[1] + '/' + id, true);
  montarShell();
  $$('#menu a').forEach(a => a.classList.toggle('active', a.dataset.id === id));
  bootstrap.Offcanvas.getInstance('#side')?.hide();
  document.title = item[2] + ' · Ebenezer Academy';
  return guard(item[3])();
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
    $('#res').insertAdjacentHTML('beforeend', `<div class="card mt-3"><div class="card-body"><h6>Testes e avaliações <small class="text-muted fw-normal">· a média do trimestre é a média dos testes desse trimestre</small></h6>${tbl(['Disciplina', 'Trimestre', 'Teste', 'Data', 'Nota'], (r.testes || []).map(t => `<tr><td>${esc(t.disciplina)}</td><td>${t.trimestre}.º T</td><td>${esc(t.titulo)}</td><td>${t.data ?? '—'}</td><td class="fw-bold ${t.valor < 10 ? 'text-danger' : ''}">${Number(t.valor).toFixed(1)}</td></tr>`))}</div></div>`);
  });
}
async function vInscricao() {
  await carregarNiveis();
  view(`${head('Inscrição online de novo aluno', 'A secretária validará a inscrição. O aluno pode ser inscrito em uma ou várias turmas.')}<div class="row g-3"><div class="col-lg-6"><form id="f" class="card card-body vstack gap-2">
    <input name="nome" class="form-control" placeholder="Nome completo" required><div class="row g-2"><div class="col"><input name="nascimento" type="date" class="form-control" required></div><div class="col"><select name="genero" class="form-select"><option>Masculino</option><option>Feminino</option></select></div></div>
    <select name="nivel" id="niv" class="form-select" required><option value="">Nível / classe pretendido…</option>${NIVEIS.map(n => `<option>${esc(n)}</option>`).join('')}</select>
    <div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba">Confirmar inscrição</button></form></div>
    <div class="col-lg-6"><div class="card card-body"><h6>Turmas compatíveis <small class="text-muted fw-normal">(pode escolher mais de uma)</small></h6><div id="tur" class="vstack gap-2 text-muted small">Escolha o nível.</div></div></div></div>`);
  $('#niv').onchange = guard(async () => {
    if (!$('#niv').value) { $('#tur').textContent = 'Escolha o nível.'; return; }
    const t = await api('/encarregado/turmas?nivel=' + encodeURIComponent($('#niv').value));
    $('#tur').innerHTML = t.map(x => `<label class="border rounded p-2 d-flex justify-content-between ${x.vagas ? '' : 'opacity-50'}"><span><input type="checkbox" name="turmaId" value="${x.id}" ${x.vagas ? '' : 'disabled'} class="form-check-input me-2">${esc(x.label)} · ${esc(x.turno || '')}<br><small class="text-muted">${x.ocupacao}/${x.capacidade} lugares</small></span>${x.vagas ? `<span class="badge text-bg-success align-self-center">${x.vagas} vagas</span>` : '<span class="badge text-bg-danger align-self-center">Sem vagas</span>'}</label>`).join('') || 'Ainda não há turmas para este nível. Contacte a secretaria.';
  });
  $('#f').onsubmit = async e => {
    e.preventDefault(); const b = Object.fromEntries(new FormData(e.target)); b.turmaIds = $$('[name=turmaId]:checked').map(x => x.value); delete b.turmaId; $('#err').classList.add('d-none');
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
    $$('[data-p]').forEach(b => b.onclick = guard(() => pagar(l.find(x => x.id == b.dataset.p), ed)));
  });
}
async function pagar(m, ed) {
  const contas = await api('/encarregado/contas-pagamento'), NOMES = {MPESA: 'M-Pesa (Vodacom)', EMOLA: 'e-Mola (Movitel)', TRANSFERENCIA: 'Transferência bancária'};
  const dest = met => { const c = contas.find(x => x.metodo === met); if (!c) return '<span class="text-muted">Conta de depósito não definida. Contacte a secretaria.</span>';
    return `<div class="small text-uppercase text-muted fw-semibold mb-1">Deposite para</div><div><span class="text-muted">Nome:</span> <b>${esc(c.titular)}</b></div>${c.banco ? `<div><span class="text-muted">Banco:</span> <b>${esc(c.banco)}</b></div>` : ''}<div><span class="text-muted">${met === 'TRANSFERENCIA' ? 'Nº da conta / NIB:' : 'Número:'}</span> <b class="fs-5" id="dnum">${esc(c.numero)}</b> <button type="button" class="btn btn-sm btn-outline-success ms-1" id="cpy"><i class="bi bi-clipboard"></i> Copiar</button></div><div class="small text-muted mt-1">Valor a depositar: <b>${MT(m.total)}</b></div>`; };
  const mod = modal(`Efetuar pagamento – ${m.mes} ${m.ano}`, `<dl class="row"><dt class="col-5">Aluno</dt><dd class="col-7">${esc(ed.nome)}</dd><dt class="col-5">Mensalidade</dt><dd class="col-7">${MT(m.valor)}</dd><dt class="col-5">Multa por atraso</dt><dd class="col-7 text-danger">${MT(m.multa)}</dd><dt class="col-5">Total a pagar</dt><dd class="col-7 fw-bold">${MT(m.total)}</dd></dl>
    ${Object.entries(NOMES).map((x, i) => `<label class="border rounded p-2 d-block mb-2"><input type="radio" name="met" value="${x[0]}" class="form-check-input me-2" ${i ? '' : 'checked'}>${x[1]}</label>`).join('')}
    <div id="dest" class="alert alert-success mb-3"></div>
    <label class="form-label small fw-semibold mb-1">O seu número de telemóvel (de onde faz o depósito)</label>
    <input id="num" class="form-control" placeholder="Ex.: 84 512 3398"><div id="perr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba w-100" id="pg"><i class="bi bi-lock"></i> Confirmar pagamento de ${MT(m.total)}</button>`);
  const atual = () => document.querySelector('[name=met]:checked').value;
  const mostrar = () => { $('#dest').innerHTML = dest(atual()); const b = $('#cpy'); if (b) b.onclick = () => { navigator.clipboard?.writeText($('#dnum').textContent); toast('Número copiado.'); }; };
  $$('[name=met]').forEach(r => r.onchange = mostrar); mostrar();
  $('#pg').onclick = async () => {
    try { const r = await api('/encarregado/pagamentos', {m: 'POST', b: {mensalidadeId: m.id, metodo: atual(), numero: $('#num').value}}); mod.hide(); toast('Pagamento confirmado. Recibo ' + r.recibo); vPagamentos(); }
    catch (e) { $('#perr').textContent = e.message; }
  };
}
async function vNotif(url) {
  const l = await api(url);
  view(head('Notificações') + `<div class="list-group">${l.map(n => `<div class="list-group-item"><i class="bi bi-bell text-eba me-2"></i>${esc(n.mensagem)} <small class="text-muted float-end">${n.data}</small></div>`).join('') || '<div class="text-muted">Sem notificações.</div>'}</div>`);
}

/* ================= SECRETÁRIA ================= */
async function vAlunos() {
  await carregarNiveis();
  const [tur, al] = await Promise.all([api('/secretaria/turmas'), api('/secretaria/alunos')]);
  const optNivel = `${NIVEIS.map(n => `<option>${esc(n)}</option>`).join('')}`;
  view(`<div class="d-flex justify-content-between">${head('Alunos e Turmas', 'Registo de alunos, encarregados e turmas · um aluno pode estar em várias turmas')}<button class="btn btn-outline-success btn-sm align-self-start" data-bs-toggle="collapse" data-bs-target="#nt"><i class="bi bi-plus"></i> Nova turma</button></div>
  <form id="nt" class="collapse card card-body mb-3"><div class="row g-2"><div class="col-6 col-md-2"><input name="codigo" class="form-control" placeholder="Código (A)" required></div><div class="col-6 col-md-3"><select name="nivel" class="form-select" required>${optNivel}</select></div><div class="col-6 col-md-2"><input name="capacidade" type="number" min="1" value="30" class="form-control"></div><div class="col-6 col-md-3"><select name="turno" class="form-select"><option>Manhã</option><option>Tarde</option></select></div><div class="col-12 col-md-2"><button class="btn btn-eba w-100">Criar</button></div></div>${NIVEIS.length ? '' : '<div class="text-danger small mt-2">Ainda não há níveis. Peça ao administrador para cadastrar os níveis.</div>'}</form>
  <div class="row g-3 mb-3"><div class="col-lg-7"><form id="fa" class="card card-body"><h6>Novo aluno</h6><div class="row g-2">
    <div class="col-12 col-md-6"><input name="nome" class="form-control" placeholder="Nome completo" required></div><div class="col-6 col-md-3"><input name="nascimento" type="date" class="form-control"></div><div class="col-6 col-md-3"><select name="genero" class="form-select"><option>Masculino</option><option>Feminino</option></select></div>
    <div class="col-12"><select name="nivel" id="nv" class="form-select" required><option value="">Nível / classe</option>${optNivel}</select></div>
    <div class="col-12"><small class="fw-semibold">Turmas (escolha uma ou mais)</small><div id="tn" class="vstack gap-1 mt-1 small text-muted">Escolha o nível para ver as turmas.</div></div>
    <div class="col-12"><small class="fw-semibold">Encarregado de educação (obrigatório)</small></div><div class="col-md-4"><input name="enome" class="form-control" placeholder="Nome"></div><div class="col-md-4"><input name="econtacto" class="form-control" placeholder="Contacto"></div><div class="col-md-4"><input name="eemail" type="email" class="form-control" placeholder="Email"></div>
    <div class="col-12"><div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba">Guardar aluno</button></div></div></form></div>
  <div class="col-lg-5"><div class="card card-body"><h6>Ocupação das turmas</h6>${tur.map(t => `<div class="small mt-2">${esc(t.label)}<span class="float-end">${t.ocupacao}/${t.capacidade}${t.vagas ? '' : ' · cheia'}</span></div><div class="progress"><div class="progress-bar ${t.vagas ? (t.ocupacao / t.capacidade > .85 ? 'bg-warning' : 'bg-success') : 'bg-danger'}" style="width:${t.ocupacao / t.capacidade * 100}%"></div></div>`).join('') || '<p class="text-muted small mb-0">Ainda não há turmas.</p>'}</div></div></div>
  <div class="card card-body">${tbl(['Aluno', 'Turmas', 'Encarregado', 'Estado', ''], al.map(a => `<tr><td>${esc(a.nome)}<br><small class="text-muted">${esc(a.nivel)}</small></td><td>${a.turmas.map(t => `<span class="badge text-bg-light border me-1">${esc(t)}</span>`).join('') || '—'}</td><td>${esc(a.encarregado)}</td><td>${a.estado === 'PENDENTE' ? '<span class="badge text-bg-warning">Pendente</span>' : bdg(a.estado)}</td><td><button class="btn btn-sm btn-outline-success" data-tt="${a.id}" title="Alterar turmas"><i class="bi bi-diagram-3"></i> Turmas</button></td></tr>`))}</div>`);
  const checks = (nivel, sel = [], nome = 'turmaId') => { const l = tur.filter(t => t.nivel === nivel);
    return l.map(t => { const ja = sel.includes(t.id), cheia = !t.vagas && !ja; return `<label class="border rounded p-2 d-flex justify-content-between ${cheia ? 'opacity-50' : ''}"><span><input type="checkbox" name="${nome}" value="${t.id}" class="form-check-input me-2" ${ja ? 'checked' : ''} ${cheia ? 'disabled' : ''}>${esc(t.label)} <small class="text-muted">· ${esc(t.turno || '')}</small></span><small class="${t.vagas ? 'text-success' : 'text-danger'}">${t.vagas ? t.vagas + ' vagas' : 'sem vagas'}</small></label>`; }).join('') || '<span class="text-danger">Não há turmas para este nível. Crie uma turma primeiro (botão "Nova turma").</span>'; };
  $('#nv').onchange = () => { $('#tn').innerHTML = $('#nv').value ? checks($('#nv').value) : 'Escolha o nível para ver as turmas.'; };
  $('#nt').onsubmit = guard(async e => { e.preventDefault(); await api('/secretaria/turmas', {m: 'POST', b: Object.fromEntries(new FormData(e.target))}); toast('Turma criada.'); vAlunos(); });
  $('#fa').onsubmit = async e => {
    e.preventDefault(); const f = Object.fromEntries(new FormData(e.target)); $('#err').classList.add('d-none');
    const b = {nome: f.nome, nascimento: f.nascimento || null, genero: f.genero, nivel: f.nivel, turmaIds: $$('#tn [name=turmaId]:checked').map(x => x.value), encarregado: {nome: f.enome, contacto: f.econtacto, email: f.eemail}};
    try { await api('/secretaria/alunos', {m: 'POST', b}); toast('Aluno registado (palavra-passe inicial do encarregado: 1234).'); vAlunos(); }
    catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); }
  };
  $$('[data-tt]').forEach(b => b.onclick = () => {
    const a = al.find(x => x.id == b.dataset.tt);
    const m = modal(`Turmas de ${a.nome}`, `<p class="small text-muted">Nível: ${esc(a.nivel)} · marque todas as turmas a que o aluno pertence.</p><div id="tm" class="vstack gap-1">${checks(a.nivel, a.turmaIds, 'tm')}</div><div id="terr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba" id="tg">Guardar turmas</button>`);
    $('#tg').onclick = async () => { try { await api(`/secretaria/alunos/${a.id}/turmas`, {m: 'PUT', b: {turmaIds: $$('#tm [name=tm]:checked').map(x => x.value)}}); m.hide(); toast('Turmas atualizadas.'); vAlunos(); } catch (e) { $('#terr').textContent = e.message; } };
  });
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
  if (!t.turmas.length) return view(`${head('Disciplina: ' + esc(t.disciplina), 'Turmas e alunos')}<div class="alert alert-warning">Ainda não tem turmas atribuídas. Peça ao administrador para lhe atribuir turmas.</div>`);
  view(`${head('Disciplina: ' + esc(t.disciplina), 'Turmas e alunos')}<div class="row g-2 mb-3"><div class="col-md-4"><select id="tu" class="form-select">${opts(t.turmas, x => x.id, x => x.label)}</select></div>${extra}</div><div id="res"></div>`);
  $$('#view select,#view input').forEach(i => i.onchange = guard(onload)); bind?.(); guard(onload)();
}
function vLancar() {
  const triAtual = (m => m <= 4 ? 1 : m <= 8 ? 2 : 3)(new Date().getMonth() + 1);
  turmaSel(`<div class="col-md-3"><select id="tri" class="form-select">${[1, 2, 3].map(i => `<option value="${i}" ${i === triAtual ? 'selected' : ''}>${i}.º Trimestre</option>`).join('')}</select></div><div class="col-md-5 d-flex gap-2"><button id="nt" class="btn btn-outline-success text-nowrap"><i class="bi bi-plus-lg"></i> Novo teste</button><button id="gu" class="btn btn-eba text-nowrap"><i class="bi bi-check"></i> Guardar notas</button></div>`, async () => {
    const tri = +$('#tri').value, turma = $('#tu').value, p = await api(`/professor/turmas/${turma}/pauta?trimestre=${tri}`), tem = p.alunos.some(a => a.legado != null);
    const vazio = !p.testes.length && !tem;
    $('#res').innerHTML = `<div class="card card-body"><p class="small text-muted mb-2">Crie quantos testes quiser em cada trimestre. A <b>média do trimestre</b> é a média de todos os testes desse trimestre.</p>
    ${vazio ? '<div class="alert alert-info py-2">Ainda não há testes neste trimestre. Clique em <b>Novo teste</b> para começar.</div>' : ''}
    ${tbl(['Aluno', ...p.testes.map(t => `<div>${esc(t.titulo)} <button class="btn btn-sm btn-link text-danger p-0 ms-1" data-dt="${t.id}" title="Apagar teste"><i class="bi bi-trash"></i></button></div><small class="text-muted fw-normal">${t.data ?? ''}</small>`), ...(tem ? ['Nota anterior'] : []), 'Média do ' + tri + '.º T', 'Situação'],
      p.alunos.map(a => `<tr data-id="${a.id}"><td>${esc(a.nome)}</td>${p.testes.map(t => `<td><input class="form-control form-control-sm nota-in" type="number" min="0" max="20" step="0.1" data-t="${t.id}" data-orig="${a.notas[t.id] ?? ''}" value="${a.notas[t.id] ?? ''}"></td>`).join('')}${tem ? `<td data-leg="${a.legado ?? ''}" class="text-muted">${a.legado ?? '–'}</td>` : ''}<td class="med fw-bold"></td><td class="sit"></td></tr>`))}
    <div id="verr" class="alert alert-danger py-2 d-none">Existem notas fora do intervalo permitido (0 a 20 valores). Corrija para poder guardar.</div></div>`;
    const calc = tr => {
      const ins = [...tr.querySelectorAll('input')]; let bad = false; const nums = [];
      ins.forEach(i => { const v = i.value === '' ? null : parseFloat(i.value), ok = v === null || (v >= 0 && v <= 20); i.classList.toggle('is-invalid', !ok); if (!ok) bad = true; else if (v !== null) nums.push(v); });
      const leg = tr.querySelector('[data-leg]'); if (leg && leg.dataset.leg !== '') nums.push(parseFloat(leg.dataset.leg));
      const m = nums.length ? nums.reduce((a, b) => a + b) / nums.length : null;       // RD04: média dos testes do trimestre
      tr.querySelector('.med').textContent = !bad && m !== null ? m.toFixed(1) : '—';
      tr.querySelector('.sit').innerHTML = bad ? '<span class="badge text-bg-danger">Valor inválido</span>' : m === null ? '' : m >= 10 ? '<span class="badge text-bg-success">Aprovado</span>' : '<span class="badge text-bg-danger">Não aprovado</span>';   // RD02
    };
    $$('#res tbody tr[data-id]').forEach(tr => { calc(tr); tr.querySelectorAll('input').forEach(i => i.oninput = () => { calc(tr); $('#verr').classList.toggle('d-none', !$$('#res .is-invalid').length); }); });
    $$('[data-dt]').forEach(b => b.onclick = guard(async () => { if (!confirm('Apagar este teste e todas as notas lançadas nele?')) return; await api('/professor/testes/' + b.dataset.dt, {m: 'DELETE'}); toast('Teste apagado.'); $('#tu').onchange(); }));
  }, () => {
    $('#nt').onclick = () => {
      const m = modal('Novo teste', `<label class="form-label small fw-semibold">Título</label><input id="tt" class="form-control mb-2" placeholder="Ex.: Teste 1, Ficha de avaliação, Teste oral…"><label class="form-label small fw-semibold">Data</label><input id="td" type="date" class="form-control" value="${new Date().toISOString().slice(0, 10)}"><div id="terr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba" id="tg">Criar teste</button>`);
      $('#tg').onclick = async () => { try { await api('/professor/testes', {m: 'POST', b: {turmaId: $('#tu').value, trimestre: $('#tri').value, titulo: $('#tt').value, data: $('#td').value}}); m.hide(); toast('Teste criado. Lance as notas e guarde.'); $('#tu').onchange(); } catch (e) { $('#terr').textContent = e.message; } };
    };
    $('#gu').onclick = guard(async () => {
      if ($$('#res .is-invalid').length) return toast('Corrija as notas inválidas (0–20).', false);
      let n = 0;
      for (const tr of $$('#res tbody tr[data-id]')) for (const i of tr.querySelectorAll('input[data-t]')) {
        if (i.value === '' || i.value === i.dataset.orig) continue;                  // só envia o que mudou
        await api('/professor/notas', {m: 'POST', b: {alunoId: tr.dataset.id, testeId: i.dataset.t, valor: i.value}}); i.dataset.orig = i.value; n++;
      }
      toast(n ? n + ' notas guardadas. Os encarregados foram notificados.' : 'Não há alterações para guardar.');
    });
  });
}

/* ---------- Material académico: professor (upload) ---------- */
async function vMaterialProf() {
  const [t, l] = await Promise.all([api('/professor/turmas'), api('/professor/materiais')]);
  if (!t.turmas.length) return view(`${head('Material académico')}<div class="alert alert-warning">Ainda não tem turmas atribuídas. Peça ao administrador para lhe atribuir turmas.</div>`);
  view(`${head('Material académico', 'Envie PDF, DOCX ou fotos às turmas. Os encarregados são notificados a cada novo envio ou atualização.')}<div class="row g-3"><div class="col-lg-4"><form id="f" class="card card-body vstack gap-2"><h6>Novo material</h6>
    <select name="turmaId" class="form-select" required>${opts(t.turmas, x => x.id, x => x.label)}</select><input name="titulo" class="form-control" placeholder="Título (ex.: Ficha de exercícios 3)" required>
    <textarea name="descricao" class="form-control" rows="2" placeholder="Descrição (opcional)"></textarea>
    <input name="ficheiro" type="file" class="form-control" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.jpg,.jpeg,.png" required><small class="text-muted">PDF, DOCX, PPTX, XLSX, JPG ou PNG · máx. 20 MB</small>
    <div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba"><i class="bi bi-cloud-upload"></i> Enviar e notificar</button></form></div>
  <div class="col-lg-8"><div class="card card-body">${tbl(['Ficheiro', 'Turma', 'Data', ''], l.map(m => `<tr><td><i class="bi ${icone(m.nomeFicheiro)} me-1"></i><b>${esc(m.titulo)}</b><br><small class="text-muted">${esc(m.nomeFicheiro)} · ${kb(m.tamanho)}${m.descricao ? ' · ' + esc(m.descricao) : ''}</small></td><td>${esc(m.turma)}</td><td>${m.atualizadoEm ? 'Atualizado ' + m.atualizadoEm : m.data}</td><td class="text-nowrap"><button class="btn btn-sm btn-outline-success" data-dl="${m.id}" title="Descarregar"><i class="bi bi-download"></i></button> <button class="btn btn-sm btn-outline-success" data-ed="${m.id}" title="Editar / substituir"><i class="bi bi-pencil"></i></button> <button class="btn btn-sm btn-outline-danger" data-rm="${m.id}" title="Apagar"><i class="bi bi-trash"></i></button></td></tr>`))}</div></div></div>`);
  $('#f').onsubmit = async e => {
    e.preventDefault(); $('#err').classList.add('d-none');
    try { await upload('/professor/materiais', new FormData(e.target)); toast('Material enviado. Encarregados notificados.'); vMaterialProf(); }
    catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); }
  };
  $$('[data-dl]').forEach(b => b.onclick = guard(() => { const m = l.find(x => x.id == b.dataset.dl); return baixar(`/professor/materiais/${m.id}/ficheiro`, m.nomeFicheiro); }));
  $$('[data-rm]').forEach(b => b.onclick = guard(async () => { if (!confirm('Apagar este material?')) return; await api('/professor/materiais/' + b.dataset.rm, {m: 'DELETE'}); toast('Material apagado.'); vMaterialProf(); }));
  $$('[data-ed]').forEach(b => b.onclick = () => {
    const x = l.find(m => m.id == b.dataset.ed);
    const mod = modal('Atualizar material', `<input id="et" class="form-control mb-2" value="${esc(x.titulo)}"><textarea id="edsc" class="form-control mb-2" rows="2" placeholder="Descrição">${esc(x.descricao ?? '')}</textarea><label class="form-label small fw-semibold">Substituir ficheiro (opcional)</label><input id="ef" type="file" class="form-control" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.jpg,.jpeg,.png"><div id="eerr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba" id="eg">Guardar e notificar</button>`);
    $('#eg').onclick = async () => {
      const fd = new FormData(); fd.append('titulo', $('#et').value); fd.append('descricao', $('#edsc').value); if ($('#ef').files[0]) fd.append('ficheiro', $('#ef').files[0]);
      try { await upload('/professor/materiais/' + x.id, fd); mod.hide(); toast('Material atualizado. Encarregados notificados.'); vMaterialProf(); } catch (e) { $('#eerr').textContent = e.message; }
    };
  });
}
/* ---------- Material académico: aluno/encarregado (descarregar) ---------- */
async function vMaterialEnc() {
  const l = await api('/encarregado/materiais');
  view(head('Material académico', 'Ficheiros enviados pelos professores às turmas dos seus educandos') + `<div class="card card-body">${tbl(['Material', 'Disciplina', 'Turma', 'Data', ''], l.map(m => `<tr><td><i class="bi ${icone(m.nomeFicheiro)} me-1"></i><b>${esc(m.titulo)}</b><br><small class="text-muted">${esc(m.nomeFicheiro)} · ${kb(m.tamanho)}${m.descricao ? ' · ' + esc(m.descricao) : ''}</small></td><td>${esc(m.disciplina)}</td><td>${esc(m.turma)}</td><td>${m.atualizadoEm ? 'Atualizado ' + m.atualizadoEm : m.data}</td><td><button class="btn btn-sm btn-eba" data-dl="${m.id}"><i class="bi bi-download"></i> Descarregar</button></td></tr>`))}</div>`);
  $$('[data-dl]').forEach(b => b.onclick = guard(() => { const m = l.find(x => x.id == b.dataset.dl); return baixar(`/encarregado/materiais/${m.id}/ficheiro`, m.nomeFicheiro); }));
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
  const l = await api('/diretor/professores'), DISC = await api('/comum/disciplinas'); let edit = null;
  view(`${head('Gestão de professores', l.length + ' professores registados')}<div class="row g-3"><div class="col-lg-8"><div class="card card-body">${tbl(['Professor', 'Especialidade', 'Contratação', 'Contacto', ''], l.map(p => `<tr><td>${esc(p.nome)}<br><small class="text-muted">${esc(p.email)}</small></td><td>${esc(p.especialidade)}</td><td>${p.dataContratacao}</td><td>${esc(p.contacto)}</td><td><button class="btn btn-sm btn-outline-success" data-e="${p.id}"><i class="bi bi-pencil"></i></button></td></tr>`))}</div></div>
  <div class="col-lg-4"><form id="f" class="card card-body vstack gap-2"><h6 id="ft">Registar novo professor</h6><input name="nome" class="form-control" placeholder="Nome completo"><select name="especialidade" class="form-select">${DISC.map(d => `<option>${esc(d.nome)}</option>`).join('') || '<option value="">Sem disciplinas cadastradas</option>'}</select><input name="dataContratacao" type="date" class="form-control"><input name="contacto" class="form-control" placeholder="Contacto"><input name="email" type="email" class="form-control" placeholder="Email (utilizador)">
  <small class="text-muted">A disciplina vem do cadastro feito pelo administrador. É criada a conta com perfil de professor (palavra-passe inicial 1234).</small><div id="err" class="alert alert-danger py-2 d-none"></div><div class="d-flex gap-2"><button type="button" class="btn btn-outline-secondary" onclick="vProf()">Cancelar</button><button class="btn btn-eba flex-grow-1">Guardar</button></div></form></div></div>`);
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

/* ================= ADMINISTRADOR ================= */
async function vAdPainel() {
  const r = await api('/admin/resumo');
  view(`${head('Painel de administração', 'Configure níveis, turmas, utilizadores e contas de pagamento do sistema')}<div class="row g-3 mb-3">${kpi('bi-layers', r.niveis, 'Níveis')}${kpi('bi-grid', r.turmas, 'Turmas')}${kpi('bi-mortarboard', r.alunos, 'Alunos')}${kpi('bi-people', r.utilizadores, 'Utilizadores')}</div>
  <div class="row g-3">${[['niveis', 'bi-layers', 'Cadastrar nível', 'Crie as classes/níveis (ex.: 10.ª Classe).'], ['turmas', 'bi-grid', 'Cadastrar turma', 'Crie turmas por nível, turno e capacidade.'], ['disciplinas', 'bi-journal-bookmark', 'Disciplinas', 'Cadastre as disciplinas e ligue-as às turmas.'], ['users', 'bi-people', 'Utilizadores', 'Crie contas, ative/desative e redefina palavras-passe.'], ['contas', 'bi-bank', 'Contas de pagamento', 'Nome e número onde os encarregados fazem o depósito.']].map(x => `<div class="col-md-6 col-xl"><a href="#" class="card h-100 text-decoration-none" data-go="${x[0]}"><div class="card-body"><i class="bi ${x[1]} fs-3 text-eba"></i><div class="fw-bold mt-2 text-dark">${x[2]}</div><small class="text-muted">${x[3]}</small></div></a></div>`).join('')}</div>`);
  $$('[data-go]').forEach(a => a.onclick = e => { e.preventDefault(); ir(a.dataset.go); });
}
async function vAdNiveis() {
  const l = await api('/admin/niveis');
  view(`${head('Níveis / classes', 'Os níveis cadastrados aparecem na criação de turmas, no registo de alunos e nas inscrições online')}<div class="row g-3"><div class="col-lg-4"><form id="f" class="card card-body vstack gap-2"><h6>Cadastrar nível</h6><input name="nome" class="form-control" placeholder="Ex.: 10.ª Classe" required><div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba">Cadastrar nível</button></form></div>
  <div class="col-lg-8"><div class="card card-body">${tbl(['Nível', 'Turmas', ''], l.map(n => `<tr><td><b>${esc(n.nome)}</b></td><td>${n.turmas}</td><td class="text-end"><button class="btn btn-sm btn-outline-danger" data-rm="${n.id}" ${n.turmas ? 'disabled title="Tem turmas"' : ''}><i class="bi bi-trash"></i></button></td></tr>`))}</div></div></div>`);
  $('#f').onsubmit = async e => { e.preventDefault(); $('#err').classList.add('d-none'); try { await api('/admin/niveis', {m: 'POST', b: Object.fromEntries(new FormData(e.target))}); toast('Nível cadastrado.'); await carregarNiveis(); vAdNiveis(); } catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); } };
  $$('[data-rm]').forEach(b => b.onclick = guard(async () => { if (!confirm('Eliminar este nível?')) return; await api('/admin/niveis/' + b.dataset.rm, {m: 'DELETE'}); toast('Nível eliminado.'); await carregarNiveis(); vAdNiveis(); }));
}
async function vAdDisciplinas() {
  const l = await api('/admin/disciplinas');
  view(`${head('Disciplinas', 'Cadastre as disciplinas; depois ligue-as às turmas e aos professores')}<div class="row g-3"><div class="col-lg-4"><form id="f" class="card card-body vstack gap-2"><h6>Cadastrar disciplina</h6><input name="nome" class="form-control" placeholder="Ex.: Matemática" required><div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba">Cadastrar disciplina</button>
    <small class="text-muted">Fluxo: <b>professor</b> → <b>disciplina</b> → <b>turma</b>. Ligue a disciplina à turma em <i>Turmas</i> e o professor à disciplina em <i>Utilizadores</i>.</small></form></div>
  <div class="col-lg-8"><div class="card card-body">${tbl(['Disciplina', 'Turmas', 'Professores', ''], l.map(d => `<tr><td><b>${esc(d.nome)}</b></td><td>${d.turmas}</td><td>${d.professores}</td><td class="text-end"><button class="btn btn-sm btn-outline-danger" data-rm="${d.id}"><i class="bi bi-trash"></i></button></td></tr>`))}</div></div></div>`);
  $('#f').onsubmit = async e => { e.preventDefault(); $('#err').classList.add('d-none'); try { await api('/admin/disciplinas', {m: 'POST', b: Object.fromEntries(new FormData(e.target))}); toast('Disciplina cadastrada.'); vAdDisciplinas(); } catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); } };
  $$('[data-rm]').forEach(b => b.onclick = guard(async () => { if (!confirm('Eliminar esta disciplina?')) return; await api('/admin/disciplinas/' + b.dataset.rm, {m: 'DELETE'}); toast('Disciplina eliminada.'); vAdDisciplinas(); }));
}
async function vAdTurmas() {
  await carregarNiveis(); const l = await api('/admin/turmas'), ano = new Date().getFullYear();
  const campos = (t = {}) => `<div class="row g-2"><div class="col-6"><label class="form-label small mb-0">Código</label><input name="codigo" class="form-control" placeholder="A" value="${esc(t.codigo ?? '')}" required></div><div class="col-6"><label class="form-label small mb-0">Nível</label><select name="nivel" class="form-select" required>${opts(NIVEIS, x => x, x => x, t.nivel)}</select></div>
    <div class="col-4"><label class="form-label small mb-0">Turno</label><select name="turno" class="form-select">${['Manhã', 'Tarde'].map(x => `<option ${x === t.turno ? 'selected' : ''}>${x}</option>`).join('')}</select></div><div class="col-4"><label class="form-label small mb-0">Ano letivo</label><input name="anoLetivo" type="number" class="form-control" value="${t.anoLetivo ?? ano}"></div><div class="col-4"><label class="form-label small mb-0">Capacidade</label><input name="capacidade" type="number" min="1" class="form-control" value="${t.capacidade ?? 30}"></div></div>`;
  view(`${head('Turmas', 'Cadastre turmas por nível, turno e capacidade')}<div class="row g-3"><div class="col-lg-4"><form id="f" class="card card-body vstack gap-2"><h6>Cadastrar turma</h6>${NIVEIS.length ? campos() : '<div class="alert alert-warning py-2 mb-0">Cadastre primeiro um nível em <a href="#" data-go="niveis">Níveis</a>.</div>'}<div id="err" class="alert alert-danger py-2 d-none"></div>${NIVEIS.length ? '<button class="btn btn-eba">Cadastrar turma</button>' : ''}</form></div>
  <div class="col-lg-8"><div class="card card-body">${tbl(['Turma', 'Turno', 'Ano', 'Ocupação', 'Disciplinas', ''], l.map(t => `<tr><td><b>${esc(t.label)}</b></td><td>${esc(t.turno ?? '')}</td><td>${t.anoLetivo}</td><td>${t.ocupacao}/${t.capacidade}</td><td>${(t.disciplinas || []).map(d => `<span class="badge text-bg-light border me-1">${esc(d)}</span>`).join('') || '<small class="text-muted">Nenhuma</small>'}</td><td class="text-end text-nowrap"><button class="btn btn-sm btn-outline-success" data-dt="${t.id}" title="Disciplinas da turma"><i class="bi bi-journal-bookmark"></i></button> <button class="btn btn-sm btn-outline-success" data-e="${t.id}"><i class="bi bi-pencil"></i></button> <button class="btn btn-sm btn-outline-danger" data-rm="${t.id}"><i class="bi bi-trash"></i></button></td></tr>`))}</div></div></div>`);
  $$('[data-go]').forEach(a => a.onclick = e => { e.preventDefault(); ir(a.dataset.go); });
  const f = $('#f'); f.onsubmit = async e => { e.preventDefault(); $('#err').classList.add('d-none'); try { await api('/admin/turmas', {m: 'POST', b: Object.fromEntries(new FormData(f))}); toast('Turma cadastrada.'); vAdTurmas(); } catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); } };
  $$('[data-rm]').forEach(b => b.onclick = guard(async () => { if (!confirm('Eliminar esta turma?')) return; await api('/admin/turmas/' + b.dataset.rm, {m: 'DELETE'}); toast('Turma eliminada.'); vAdTurmas(); }));
  $$('[data-dt]').forEach(b => b.onclick = guard(async () => {
    const t = l.find(x => x.id == b.dataset.dt), todas = await api('/admin/disciplinas');
    const m = modal('Disciplinas de ' + t.label, `<p class="small text-muted">Marque as disciplinas leccionadas nesta turma. Só os professores dessas disciplinas podem ser atribuídos à turma.</p><div id="dd" class="vstack gap-1">${todas.map(d => `<label class="border rounded p-2"><input type="checkbox" name="dd" value="${d.id}" class="form-check-input me-2" ${(t.disciplinaIds || []).includes(d.id) ? 'checked' : ''}>${esc(d.nome)}</label>`).join('') || '<span class="text-muted">Ainda não há disciplinas. Cadastre-as em <b>Disciplinas</b>.</span>'}</div><div id="derr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba" id="dg">Guardar disciplinas</button>`);
    $('#dg').onclick = async () => { try { const r = await api(`/admin/turmas/${t.id}/disciplinas`, {m: 'PUT', b: {disciplinaIds: $$('#dd [name=dd]:checked').map(x => x.value)}}); m.hide(); toast(r.professoresDesligados ? `Disciplinas guardadas. ${r.professoresDesligados} professor(es) desligado(s) da turma por já não terem disciplina nela.` : 'Disciplinas da turma atualizadas.'); vAdTurmas(); } catch (e) { $('#derr').textContent = e.message; } };
  }));
  $$('[data-e]').forEach(b => b.onclick = () => {
    const t = l.find(x => x.id == b.dataset.e);
    const m = modal('Editar turma', `<form id="fe">${campos(t)}</form><div id="eerr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba" id="eg">Guardar</button>`);
    $('#eg').onclick = async () => { try { await api('/admin/turmas/' + t.id, {m: 'PUT', b: Object.fromEntries(new FormData($('#fe')))}); m.hide(); toast('Turma atualizada.'); vAdTurmas(); } catch (e) { $('#eerr').textContent = e.message; } };
  });
}
async function vAdUsers() {
  const l = await api('/admin/utilizadores'), DISC = await api('/admin/disciplinas'); let filtro = window._pf ?? '';
  const lista = l.filter(u => !filtro || u.perfil === filtro);
  view(`${head('Utilizadores', l.length + ' contas no sistema')}<div class="row g-3"><div class="col-lg-4"><form id="f" class="card card-body vstack gap-2"><h6>Nova conta</h6><input name="nome" class="form-control" placeholder="Nome completo" required><input name="email" type="email" class="form-control" placeholder="Email (utilizador)" required>
    <select name="perfil" id="np" class="form-select">${Object.entries(PERFIS).map(([k, v]) => `<option value="${k}" ${k === 'PROFESSOR' ? 'selected' : ''}>${v}</option>`).join('')}</select>${DISC.length ? `<select name="especialidade" id="ne" class="form-select">${DISC.map(d => `<option>${esc(d.nome)}</option>`).join('')}</select>` : '<div id="ne" class="alert alert-warning py-2 mb-0 small">Para criar professores, cadastre primeiro as disciplinas em <a href="#" data-go="disciplinas">Disciplinas</a>.</div>'}<input name="contacto" class="form-control" placeholder="Contacto">
    <small class="text-muted">Palavra-passe inicial: 1234.</small><div id="err" class="alert alert-danger py-2 d-none"></div><button class="btn btn-eba">Criar conta</button></form></div>
  <div class="col-lg-8"><div class="card card-body"><select id="pf" class="form-select form-select-sm mb-2" style="max-width:240px"><option value="">Todos os perfis</option>${Object.entries(PERFIS).map(([k, v]) => `<option value="${k}" ${k === filtro ? 'selected' : ''}>${v}</option>`).join('')}</select>
  ${tbl(['Nome', 'Perfil', 'Estado', ''], lista.map(u => `<tr><td>${esc(u.nome)}<br><small class="text-muted">${esc(u.email)}${u.especialidade ? ' · ' + esc(u.especialidade) : ''}</small></td><td>${PERFIS[u.perfil] ?? u.perfil}</td><td>${u.ativo ? '<span class="badge text-bg-success">Ativo</span>' : '<span class="badge text-bg-secondary">Inativo</span>'}</td><td class="text-end text-nowrap">${u.perfil === 'PROFESSOR' ? `<button class="btn btn-sm btn-outline-success" data-pt="${u.id}" title="Atribuir turmas"><i class="bi bi-diagram-3"></i> Turmas (${(u.turmaIds || []).length})</button> ` : ''}<button class="btn btn-sm btn-outline-secondary" data-pw="${u.id}" title="Redefinir palavra-passe"><i class="bi bi-key"></i></button> <button class="btn btn-sm ${u.ativo ? 'btn-outline-danger' : 'btn-outline-success'}" data-at="${u.id}">${u.ativo ? 'Desativar' : 'Ativar'}</button></td></tr>`))}</div></div></div>`);
  $$('[data-go]').forEach(a => a.onclick = e => { e.preventDefault(); ir(a.dataset.go); });
  const ne = () => $('#ne').classList.toggle('d-none', $('#np').value !== 'PROFESSOR'); $('#np').onchange = ne; ne();
  $('#pf').onchange = () => { window._pf = $('#pf').value; vAdUsers(); };
  $('#f').onsubmit = async e => { e.preventDefault(); $('#err').classList.add('d-none'); try { await api('/admin/utilizadores', {m: 'POST', b: Object.fromEntries(new FormData(e.target))}); toast('Conta criada (palavra-passe 1234).'); vAdUsers(); } catch (er) { $('#err').textContent = er.message; $('#err').classList.remove('d-none'); } };
  $$('[data-pt]').forEach(b => b.onclick = guard(async () => {
    const u = l.find(x => x.id == b.dataset.pt), tur = (await api('/admin/turmas')).filter(t => (t.disciplinas || []).some(d => d.toLowerCase() === String(u.especialidade ?? '').toLowerCase()));
    const m = modal('Turmas de ' + u.nome, `<p class="small text-muted">Disciplina: ${esc(u.especialidade ?? '—')} · só aparecem as turmas em que esta disciplina é leccionada. O professor só vê e lança notas/material nas turmas marcadas.</p><div id="tp" class="vstack gap-1">${tur.map(t => `<label class="border rounded p-2"><input type="checkbox" name="tp" value="${t.id}" class="form-check-input me-2" ${(u.turmaIds || []).includes(t.id) ? 'checked' : ''}>${esc(t.label)} <small class="text-muted">· ${esc(t.turno ?? '')} · ${t.ocupacao} alunos</small></label>`).join('') || '<span class="text-muted">Nenhuma turma tem esta disciplina. Ligue-a a uma turma em <b>Turmas</b> (botão do livro).</span>'}</div><div id="perr" class="text-danger small mt-2"></div>`, `<button class="btn btn-eba" id="tg">Guardar turmas</button>`);
    $('#tg').onclick = async () => { try { await api(`/admin/utilizadores/${u.id}/turmas`, {m: 'PUT', b: {turmaIds: $$('#tp [name=tp]:checked').map(x => x.value)}}); m.hide(); toast('Turmas atribuídas.'); vAdUsers(); } catch (e) { $('#perr').textContent = e.message; } };
  }));
  $$('[data-pw]').forEach(b => b.onclick = guard(async () => { if (!confirm('Redefinir a palavra-passe para 1234?')) return; const r = await api(`/admin/utilizadores/${b.dataset.pw}/redefinir-senha`, {m: 'POST'}); toast(r.mensagem); }));
  $$('[data-at]').forEach(b => b.onclick = guard(async () => { const u = l.find(x => x.id == b.dataset.at); await api('/admin/utilizadores/' + u.id, {m: 'PUT', b: {ativo: !u.ativo}}); toast(u.ativo ? 'Conta desativada.' : 'Conta ativada.'); vAdUsers(); }));
}
async function vAdContas() {
  const l = await api('/admin/contas'), NOMES = {MPESA: 'M-Pesa (Vodacom)', EMOLA: 'e-Mola (Movitel)', TRANSFERENCIA: 'Transferência bancária'};
  view(`${head('Contas de pagamento', 'É este nome e número que o encarregado vê ao pagar a mensalidade')}<div class="row g-3">${['MPESA', 'EMOLA', 'TRANSFERENCIA'].map(k => l.find(x => x.metodo === k)).filter(Boolean).map(c => `<div class="col-lg-4"><form class="card card-body vstack gap-2" data-c="${c.id}"><h6>${NOMES[c.metodo] ?? c.metodo}</h6>
    <label class="small text-muted mb-0">Nome do titular</label><input name="titular" class="form-control" value="${esc(c.titular)}" required><label class="small text-muted mb-0">${c.metodo === 'TRANSFERENCIA' ? 'Nº da conta / NIB' : 'Número'}</label><input name="numero" class="form-control" value="${esc(c.numero)}" required>
    ${c.metodo === 'TRANSFERENCIA' ? `<label class="small text-muted mb-0">Banco</label><input name="banco" class="form-control" value="${esc(c.banco ?? '')}">` : ''}<button class="btn btn-eba">Guardar</button></form></div>`).join('')}</div>`);
  $$('form[data-c]').forEach(f => f.onsubmit = guard(async e => { e.preventDefault(); await api('/admin/contas/' + f.dataset.c, {m: 'PUT', b: Object.fromEntries(new FormData(f))}); toast('Conta guardada.'); }));
}

(async () => { if (S) { try { await carregarNiveis(); } catch { } } route(); })();
