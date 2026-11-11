/* FrioOS: estado, persistência e interface do painel. */
const FUSO = 'America/Sao_Paulo';
const DIA = 86_400_000;
const GARANTIA_DIAS = 90;
const CHAVE = 'ordens-servico.v2';
const STATUS_VALIDOS = new Set(['ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA']);
const FILTROS = { todas: 'Todas', ABERTA: 'Abertas', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluídas', atrasadas: 'Atrasadas', garantia: 'Em garantia', CANCELADA: 'Canceladas' };
const NOMES_STATUS = { ABERTA: 'Aberta', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluída', CANCELADA: 'Cancelada' };

let ordens = [];
let filtro = 'todas';
let itemDaOrdem = null;
const $ = (id) => document.getElementById(id);
const agora = () => new Date();

// Utilitários --------------------------------------------------------------
const esc = (valor) => String(valor ?? '').replace(/[&<>"']/g, (caractere) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[caractere]));
const numero = (valor, minimo = 0) => Math.max(minimo, Number(valor) || 0);
const dataValida = (valor) => { const data = new Date(valor); return Number.isNaN(data.getTime()) ? null : data; };
const dataIso = (valor, alternativa = new Date().toISOString()) => (dataValida(valor) || new Date(alternativa)).toISOString();
const formatarData = (valor) => { const data = dataValida(valor); return data ? data.toLocaleString('pt-BR', { timeZone: FUSO, dateStyle: 'short', timeStyle: 'short' }) : '—'; };
const moeda = (valor) => numero(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const total = (ordem) => ordem.itens.reduce((soma, item) => soma + item.quantidade * item.preco, 0);
const prazoFinal = (ordem) => new Date(dataValida(ordem.abertura).getTime() + ordem.prazoDias * DIA);
const estaAtiva = (ordem) => ordem.status === 'ABERTA' || ordem.status === 'EM_ANDAMENTO';
const estaAtrasada = (ordem, momento) => estaAtiva(ordem) && momento > prazoFinal(ordem);
const emGarantia = (ordem, momento) => Boolean(ordem.garantiaAte && momento < dataValida(ordem.garantiaAte));
const diasEntre = (inicio, fim) => Math.max(0, Math.floor((fim - inicio) / DIA));

function normalizarOrdem(ordem, indice) {
  const abertura = dataIso(ordem?.abertura);
  const status = STATUS_VALIDOS.has(ordem?.status) ? ordem.status : 'ABERTA';
  const conclusao = ordem?.conclusao && dataValida(ordem.conclusao) ? dataIso(ordem.conclusao) : null;
  const garantiaAte = ordem?.garantiaAte && dataValida(ordem.garantiaAte) ? dataIso(ordem.garantiaAte) : conclusao && status === 'CONCLUIDA' ? new Date(dataValida(conclusao).getTime() + GARANTIA_DIAS * DIA).toISOString() : null;
  return {
    id: Math.max(1, Math.trunc(numero(ordem?.id, indice + 1))), cliente: String(ordem?.cliente || 'Cliente não informado').trim(), telefone: String(ordem?.telefone || 'Não informado').trim(), tipo: String(ordem?.tipo || 'OUTRO').trim(), modelo: String(ordem?.modelo || 'Modelo não informado').trim(), defeito: String(ordem?.defeito || 'Sem descrição').trim(), status, prazoDias: Math.max(1, Math.trunc(numero(ordem?.prazoDias, 3))), abertura, conclusao, garantiaAte,
    itens: Array.isArray(ordem?.itens) ? ordem.itens.map((item) => ({ descricao: String(item?.descricao || 'Item sem descrição').trim(), quantidade: Math.max(1, Math.trunc(numero(item?.quantidade, 1))), preco: numero(item?.preco) })) : []
  };
}

// Persistência -------------------------------------------------------------
function carregar() {
  try { const dados = JSON.parse(localStorage.getItem(CHAVE)); return Array.isArray(dados) ? dados.map(normalizarOrdem) : []; } catch { return []; }
}
function salvar() {
  try { localStorage.setItem(CHAVE, JSON.stringify(ordens)); } catch { /* O painel continua utilizável se o armazenamento for bloqueado. */ }
  desenhar();
}

// Filtros e apresentação ---------------------------------------------------
function correspondeAoFiltro(ordem, momento) {
  const busca = $('busca').value.trim().toLocaleLowerCase('pt-BR');
  const pesquisavel = `${ordem.id} ${ordem.cliente} ${ordem.telefone} ${ordem.tipo} ${ordem.modelo} ${ordem.defeito}`.toLocaleLowerCase('pt-BR');
  if (busca && !pesquisavel.includes(busca)) return false;
  if (filtro === 'todas') return true;
  if (filtro === 'atrasadas') return estaAtrasada(ordem, momento);
  if (filtro === 'garantia') return emGarantia(ordem, momento);
  return ordem.status === filtro;
}
function mensagemDeStatus(ordem, momento) {
  if (estaAtrasada(ordem, momento)) return `<p class="status-message is-danger">${diasEntre(prazoFinal(ordem), momento)} dia(s) de atraso</p>`;
  if (!ordem.garantiaAte) return '';
  if (emGarantia(ordem, momento)) return `<p class="status-message is-success">Garantia até ${formatarData(ordem.garantiaAte)} · ${diasEntre(momento, dataValida(ordem.garantiaAte))} dias</p>`;
  return `<p class="status-message is-danger">Garantia vencida em ${formatarData(ordem.garantiaAte)}</p>`;
}
function itensHtml(ordem) {
  if (!ordem.itens.length) return '<p class="items-empty">Nenhum item adicionado</p>';
  const linhas = ordem.itens.map((item) => `<div class="item-row"><span>${item.quantidade}× ${esc(item.descricao)}</span><span>${moeda(item.quantidade * item.preco)}</span></div>`).join('');
  return `<details><summary>${ordem.itens.length} ${ordem.itens.length === 1 ? 'item' : 'itens'} no orçamento</summary><div class="item-list">${linhas}</div></details>`;
}
function acoesHtml(ordem) {
  const botoes = [];
  if (estaAtiva(ordem)) botoes.push(`<button class="button" type="button" data-acao="item" data-id="${ordem.id}">Adicionar item</button>`);
  if (ordem.status === 'ABERTA') botoes.push(`<button class="button button-primary" type="button" data-acao="iniciar" data-id="${ordem.id}">Iniciar</button>`);
  if (ordem.status === 'EM_ANDAMENTO') botoes.push(`<button class="button button-primary" type="button" data-acao="concluir" data-id="${ordem.id}">Concluir</button>`);
  if (estaAtiva(ordem)) botoes.push(`<button class="button button-danger" type="button" data-acao="cancelar" data-id="${ordem.id}">Cancelar</button>`);
  return botoes.length ? `<div class="order-actions">${botoes.join('')}</div>` : '';
}
function cartaoHtml(ordem, momento) {
  return `<article class="order-card status-${ordem.status}">
    <div class="order-top"><h3 class="order-title"><span class="order-id">#${ordem.id}</span> ${esc(ordem.cliente)}</h3><span class="status-tag tag-${ordem.status}">${NOMES_STATUS[ordem.status]}</span></div>
    <p class="equipment">${esc(ordem.tipo)} · ${esc(ordem.modelo)}</p><p class="order-description" title="${esc(ordem.defeito)}">${esc(ordem.defeito)} · ${esc(ordem.telefone)}</p>
    <p class="order-date">Aberta em ${formatarData(ordem.abertura)} · Prazo ${formatarData(prazoFinal(ordem))}</p>${mensagemDeStatus(ordem, momento)}${itensHtml(ordem)}
    <div class="order-total"><span>Total</span><strong>${moeda(total(ordem))}</strong></div>${acoesHtml(ordem)}
  </article>`;
}
function desenhar() {
  const momento = agora();
  const visiveis = ordens.filter((ordem) => correspondeAoFiltro(ordem, momento)).sort((a, b) => b.id - a.id);
  const faturado = ordens.filter((ordem) => ordem.status === 'CONCLUIDA').reduce((soma, ordem) => soma + total(ordem), 0);
  const estatisticas = [['Em aberto', ordens.filter(estaAtiva).length, ''], ['Atrasadas', ordens.filter((ordem) => estaAtrasada(ordem, momento)).length, 'stat-danger'], ['Em garantia', ordens.filter((ordem) => emGarantia(ordem, momento)).length, 'stat-success'], ['Faturado', moeda(faturado), '']];
  $('chips').innerHTML = Object.entries(FILTROS).map(([chave, nome]) => `<button class="filter-chip" type="button" data-filtro="${chave}" aria-pressed="${chave === filtro}">${nome}</button>`).join('');
  $('stats').innerHTML = estatisticas.map(([titulo, valor, classe]) => `<article class="stat ${classe}"><small>${titulo}</small><strong>${valor}</strong></article>`).join('');
  $('contador').textContent = `${visiveis.length} ${visiveis.length === 1 ? 'ordem encontrada' : 'ordens encontradas'}`;
  $('lista').innerHTML = visiveis.map((ordem) => cartaoHtml(ordem, momento)).join('') || `<div class="empty-state"><strong>${ordens.length ? 'Nenhuma ordem encontrada' : 'Sua agenda está vazia'}</strong>${ordens.length ? 'Ajuste os filtros ou a busca para ver outras ordens.' : 'Crie uma nova ordem ou carregue os dados de exemplo.'}</div>`;
}

// Ações --------------------------------------------------------------------
const encontrar = (id) => ordens.find((ordem) => ordem.id === Number(id));
const proximoId = () => ordens.reduce((maior, ordem) => Math.max(maior, ordem.id), 0) + 1;
$('lista').addEventListener('click', (evento) => {
  const botao = evento.target.closest('button[data-acao]');
  if (!botao) return;
  const ordem = encontrar(botao.dataset.id);
  if (!ordem) return;
  switch (botao.dataset.acao) {
    case 'item': itemDaOrdem = ordem.id; $('fItem').reset(); $('dItem').showModal(); return;
    case 'iniciar': ordem.status = 'EM_ANDAMENTO'; break;
    case 'concluir': ordem.status = 'CONCLUIDA'; ordem.conclusao = agora().toISOString(); ordem.garantiaAte = new Date(dataValida(ordem.conclusao).getTime() + GARANTIA_DIAS * DIA).toISOString(); break;
    case 'cancelar': if (!confirm(`Cancelar a ordem #${ordem.id}?`)) return; ordem.status = 'CANCELADA'; break;
  }
  salvar();
});
$('chips').addEventListener('click', (evento) => { const botao = evento.target.closest('[data-filtro]'); if (botao) { filtro = botao.dataset.filtro; desenhar(); } });
$('busca').addEventListener('input', desenhar);
$('bNova').addEventListener('click', () => { $('fNova').reset(); $('dNova').showModal(); });
$('bDados').addEventListener('click', () => { $('json').value = JSON.stringify(ordens, null, 2); $('erro').textContent = ''; $('dDados').showModal(); });
document.querySelectorAll('[data-fechar]').forEach((botao) => botao.addEventListener('click', () => botao.closest('dialog').close()));
$('fNova').addEventListener('submit', (evento) => {
  evento.preventDefault();
  ordens.push(normalizarOrdem({ id: proximoId(), cliente: $('nNome').value, telefone: $('nTel').value, tipo: $('nTipo').value, modelo: $('nModelo').value, defeito: $('nDefeito').value, prazoDias: $('nPrazo').value, status: 'ABERTA', abertura: agora().toISOString(), itens: [] }));
  $('dNova').close(); salvar();
});
$('fItem').addEventListener('submit', (evento) => {
  evento.preventDefault(); const ordem = encontrar(itemDaOrdem); if (!ordem) return;
  ordem.itens.push({ descricao: $('iDesc').value.trim(), quantidade: Math.trunc(numero($('iQtd').value, 1)), preco: numero($('iPreco').value) });
  $('dItem').close(); salvar();
});

// Importação e exportação --------------------------------------------------
function importar(texto) {
  try {
    const dados = JSON.parse(texto);
    if (!Array.isArray(dados)) throw new Error('o arquivo deve conter uma lista de ordens');
    ordens = dados.map(normalizarOrdem); $('erro').textContent = ''; $('dDados').close(); salvar();
  } catch (erro) { $('erro').textContent = `JSON inválido: ${erro.message}`; }
}
$('bImp').addEventListener('click', () => importar($('json').value));
$('bExp').addEventListener('click', () => { const arquivo = new Blob([JSON.stringify(ordens, null, 2)], { type: 'application/json' }); const url = URL.createObjectURL(arquivo); const link = document.createElement('a'); link.href = url; link.download = 'ordens-servico.json'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 0); });
$('bLimpa').addEventListener('click', () => { if (confirm('Apagar todas as ordens salvas neste navegador?')) { ordens = []; $('dDados').close(); salvar(); } });
$('bEx').addEventListener('click', () => {
  const referencia = Date.now(); const antes = (dias) => new Date(referencia - dias * DIA).toISOString();
  importar(JSON.stringify([
    { id: 1, cliente: 'João Pereira', telefone: '31 99999-1111', tipo: 'FREEZER', modelo: 'Consul CHB', defeito: 'Não congela', status: 'CONCLUIDA', prazoDias: 3, abertura: antes(12), conclusao: antes(9), itens: [{ descricao: 'Compressor', quantidade: 1, preco: 450 }, { descricao: 'Gás refrigerante', quantidade: 1, preco: 120 }] },
    { id: 2, cliente: 'Maria Souza', telefone: '31 98888-2222', tipo: 'GELADEIRA', modelo: 'Brastemp BRM44', defeito: 'Faz barulho', status: 'CONCLUIDA', prazoDias: 3, abertura: antes(20), conclusao: antes(18), itens: [{ descricao: 'Termostato', quantidade: 1, preco: 85 }] },
    { id: 3, cliente: 'Padaria Bom Pão', telefone: '31 97777-3333', tipo: 'EXPOSITOR', modelo: 'Metalfrio VB40', defeito: 'Não liga', status: 'EM_ANDAMENTO', prazoDias: 3, abertura: antes(6), itens: [{ descricao: 'Placa eletrônica', quantidade: 1, preco: 230 }] },
    { id: 4, cliente: 'Carlos Lima', telefone: '31 96666-4444', tipo: 'GELADEIRA', modelo: 'Electrolux DF36', defeito: 'Gelo no fundo', status: 'ABERTA', prazoDias: 5, abertura: antes(1), itens: [] }
  ]));
});

ordens = carregar();
desenhar();
