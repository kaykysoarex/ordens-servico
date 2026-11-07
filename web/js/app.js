// FrioOS: lógica do painel (dados, regras de garantia/prazo e telas)
const FUSO = 'America/Sao_Paulo', DIA = 864e5, GARANTIA_DIAS = 90, CHAVE = 'ordens-servico.v2';
const FILTROS = { todas: 'Todas', ABERTA: 'Abertas', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluídas', atrasadas: 'Atrasadas', garantia: 'Em garantia', CANCELADA: 'Canceladas' };
const NOMES = { ABERTA: 'Aberta', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluída', CANCELADA: 'Cancelada' };
let ordens = [], filtro = 'todas', itemDe = null;
const $ = id => document.getElementById(id);

// ---- Persistência (localStorage; try/catch porque o navegador pode bloquear) ----
function carregarLS() { try { return JSON.parse(localStorage.getItem(CHAVE)) || []; } catch (e) { return []; } }
function salvar() { try { localStorage.setItem(CHAVE, JSON.stringify(ordens)); } catch (e) { } desenhar(); }

// ---- Datas: ISO 8601 em UTC (igual ao Instant do Java), exibidas em São Paulo ----
const paraData = iso => new Date(iso.replace(/(\.\d{3})\d+/, '$1'));
const fmtData = d => d ? new Date(d).toLocaleString('pt-BR', { timeZone: FUSO, dateStyle: 'short', timeStyle: 'short' }) : '—';
const moeda = v => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const prazoFinal = o => new Date(paraData(o.abertura).getTime() + o.prazoDias * DIA);
const ativa = o => o.status === 'ABERTA' || o.status === 'EM_ANDAMENTO';
const atrasada = (o, agora) => ativa(o) && agora > prazoFinal(o);
const emGarantia = (o, agora) => o.garantiaAte && agora < paraData(o.garantiaAte);
const diasEntre = (a, b) => Math.floor((b - a) / DIA);
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const total = o => o.itens.reduce((s, i) => s + i.quantidade * i.preco, 0);

function passa(o, agora) {
  const q = $('busca').value.trim().toLowerCase();
  if (q && !`${o.id} ${o.cliente} ${o.modelo} ${o.defeito} ${o.tipo}`.toLowerCase().includes(q)) return false;
  if (filtro === 'todas') return true;
  if (filtro === 'atrasadas') return atrasada(o, agora);
  if (filtro === 'garantia') return emGarantia(o, agora);
  return o.status === filtro;
}

function cartao(o, agora) {
  let prazo = `Prazo ${fmtData(prazoFinal(o))}`;
  if (atrasada(o, agora)) prazo += ` <span class="bad">· ${diasEntre(prazoFinal(o), agora)} dia(s) de atraso</span>`;
  let gar = '';
  if (o.garantiaAte) gar = emGarantia(o, agora)
    ? `<div class="good">Garantia ativa até ${fmtData(o.garantiaAte)} (${diasEntre(agora, paraData(o.garantiaAte))} dias)</div>`
    : `<div class="bad">Garantia vencida em ${fmtData(o.garantiaAte)}</div>`;
  const itens = o.itens.length
    ? `<details><summary>${o.itens.length} item(ns)</summary>${o.itens.map(i => `<div class="row"><span>${i.quantidade}× ${esc(i.descricao)}</span><span>${moeda(i.quantidade * i.preco)}</span></div>`).join('')}</details>`
    : '<small>Sem itens</small>';
  const b = [];
  if (ativa(o)) b.push(`<button data-a="item" data-id="${o.id}">＋ Item</button>`);
  if (o.status === 'ABERTA') b.push(`<button class="pri" data-a="iniciar" data-id="${o.id}">Iniciar</button>`);
  if (o.status === 'EM_ANDAMENTO') b.push(`<button class="pri" data-a="concluir" data-id="${o.id}">Concluir</button>`);
  if (ativa(o)) b.push(`<button class="perigo" data-a="cancelar" data-id="${o.id}">Cancelar</button>`);
  return `<article class="os s-${o.status}">
    <div class="row"><h2>#${o.id} · ${esc(o.cliente)}</h2><span class="tag ${o.status}">${NOMES[o.status]}</span></div>
    <div>${esc(o.tipo)} — ${esc(o.modelo)}</div><small>${esc(o.defeito)} · ${esc(o.telefone)}</small>
    <small>Aberta ${fmtData(o.abertura)} · ${prazo}</small>${gar}${itens}
    <div class="row"><b>${moeda(total(o))}</b></div><div class="acoes">${b.join('')}</div></article>`;
}

function desenhar() {
  const agora = new Date();
  $('chips').innerHTML = Object.entries(FILTROS).map(([k, v]) => `<button class="chip" data-f="${k}" aria-pressed="${k === filtro}">${v}</button>`).join('');
  const fat = ordens.filter(o => o.status === 'CONCLUIDA').reduce((s, o) => s + total(o), 0);
  $('stats').innerHTML = [
    ['Em aberto', ordens.filter(ativa).length, ''], ['Atrasadas', ordens.filter(o => atrasada(o, agora)).length, 'bad'],
    ['Em garantia', ordens.filter(o => emGarantia(o, agora)).length, 'good'], ['Faturado', moeda(fat), '']
  ].map(([t, v, c]) => `<div class="stat"><small>${t}</small><b class="${c}">${v}</b></div>`).join('');
  const vis = ordens.filter(o => passa(o, agora)).sort((a, b) => b.id - a.id);
  $('lista').innerHTML = vis.map(o => cartao(o, agora)).join('') ||
    `<div class="vazio">${ordens.length ? 'Nenhuma ordem neste filtro.' : 'Nenhuma ordem ainda. Toque em “Nova ordem” ou carregue o exemplo em “Dados”.'}</div>`;
}

// ---- Ações ----
const achar = id => ordens.find(o => o.id === Number(id));
$('lista').addEventListener('click', e => {
  const bt = e.target.closest('button[data-a]'); if (!bt) return;
  const o = achar(bt.dataset.id), a = bt.dataset.a;
  if (a === 'item') { itemDe = o.id; $('fItem').reset(); $('dItem').showModal(); return; }
  if (a === 'iniciar') o.status = 'EM_ANDAMENTO';
  if (a === 'concluir') { o.status = 'CONCLUIDA'; o.conclusao = new Date().toISOString(); o.garantiaAte = new Date(Date.now() + GARANTIA_DIAS * DIA).toISOString(); }
  if (a === 'cancelar') { if (!confirm(`Cancelar a ordem #${o.id}?`)) return; o.status = 'CANCELADA'; }
  salvar();
});
$('chips').addEventListener('click', e => { const c = e.target.closest('[data-f]'); if (c) { filtro = c.dataset.f; desenhar(); } });
$('busca').addEventListener('input', desenhar);
$('bNova').onclick = () => { $('fNova').reset(); $('dNova').showModal(); };
$('bDados').onclick = () => { $('json').value = JSON.stringify(ordens, null, 2); $('erro').textContent = ''; $('dDados').showModal(); };
document.querySelectorAll('[data-fechar]').forEach(b => b.onclick = () => b.closest('dialog').close());

$('fNova').addEventListener('submit', () => {
  ordens.push({
    id: ordens.reduce((m, o) => Math.max(m, o.id), 0) + 1, cliente: $('nNome').value.trim(), telefone: $('nTel').value.trim(),
    tipo: $('nTipo').value, modelo: $('nModelo').value.trim(), defeito: $('nDefeito').value.trim(), status: 'ABERTA',
    prazoDias: Number($('nPrazo').value), abertura: new Date().toISOString(), conclusao: null, garantiaAte: null, total: 0, itens: []
  });
  salvar();
});
$('fItem').addEventListener('submit', () => {
  achar(itemDe).itens.push({ descricao: $('iDesc').value.trim(), quantidade: Number($('iQtd').value), preco: Number($('iPreco').value) });
  salvar();
});

// ---- Importar / exportar (mesmo formato do ExportadorJson do Java) ----
function importar(texto) {
  try {
    const d = JSON.parse(texto);
    if (!Array.isArray(d)) throw new Error('precisa ser uma lista de ordens');
    ordens = d.map(o => ({ ...o, itens: o.itens || [], conclusao: o.conclusao ?? null, garantiaAte: o.garantiaAte ?? null }));
    $('dDados').close(); salvar();
  } catch (e) { $('erro').textContent = 'JSON inválido: ' + e.message; }
}
$('bImp').onclick = () => importar($('json').value);
$('bExp').onclick = () => {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([JSON.stringify(ordens, null, 2)], { type: 'application/json' }));
  a.download = 'ordens-servico.json'; a.click(); URL.revokeObjectURL(a.href);
};
$('bLimpa').onclick = () => { if (confirm('Apagar todas as ordens deste navegador?')) { ordens = []; $('dDados').close(); salvar(); } };
$('bEx').onclick = () => {
  const ag = Date.now(), atras = d => new Date(ag - d * DIA).toISOString(), apos = (d, n) => new Date(ag + (n - d) * DIA).toISOString();
  importar(JSON.stringify([
    { id: 1, cliente: 'João Pereira', telefone: '31 99999-1111', tipo: 'FREEZER', modelo: 'Consul CHB', defeito: 'Não congela', status: 'CONCLUIDA', prazoDias: 3, abertura: atras(100), conclusao: atras(95), garantiaAte: apos(95, 90), itens: [{ descricao: 'Compressor', quantidade: 1, preco: 450 }, { descricao: 'Gás refrigerante', quantidade: 1, preco: 120 }] },
    { id: 2, cliente: 'Maria Souza', telefone: '31 98888-2222', tipo: 'GELADEIRA', modelo: 'Brastemp BRM44', defeito: 'Faz barulho', status: 'CONCLUIDA', prazoDias: 3, abertura: atras(20), conclusao: atras(18), garantiaAte: apos(18, 90), itens: [{ descricao: 'Termostato', quantidade: 1, preco: 85 }] },
    { id: 3, cliente: 'Padaria Bom Pão', telefone: '31 97777-3333', tipo: 'EXPOSITOR', modelo: 'Metalfrio VB40', defeito: 'Não liga', status: 'EM_ANDAMENTO', prazoDias: 3, abertura: atras(6), itens: [{ descricao: 'Placa eletrônica', quantidade: 1, preco: 230 }] },
    { id: 4, cliente: 'Carlos Lima', telefone: '31 96666-4444', tipo: 'GELADEIRA', modelo: 'Electrolux DF36', defeito: 'Gelo no fundo', status: 'ABERTA', prazoDias: 5, abertura: atras(1), itens: [] }
  ]));
};

ordens = carregarLS();
desenhar();
