// Questo file contiene tutta la logica client-side per l'interfaccia del gioco di Blackjack (Frontend).
// Gestisce autenticazione, navigazione, interazione con i bottoni di gioco, rendering delle carte e comunicazione con il server tramite API REST.

// Mettere IP locale se si accede da browser sullo stesso PC, altrimenti IP della macchina sulla rete locale (es. 192.168.1.x)
const SERVER = 'http://172.20.10.6:6767';  


// ═══════════════════════════════════════════════════════════════════
//  STATE
// ═══════════════════════════════════════════════════════════════════
let token    = sessionStorage.getItem('bj_token')    || null;
let username = sessionStorage.getItem('bj_username') || '';
let credits  = parseFloat(sessionStorage.getItem('bj_credits')) || 0;
let currentBet = 10;

// Ripristina sessione se già loggato (refresh)
if (token) {
    showApp();
    syncGameState();
}

// ═══════════════════════════════════════════════════════════════════
//  AUTH
// ═══════════════════════════════════════════════════════════════════
function switchTab(tab) {
    document.getElementById('tabLogin').classList.toggle('active', tab === 'login');
    document.getElementById('tabReg').classList.toggle('active',   tab === 'register');
    document.getElementById('formLogin').style.display    = tab === 'login'    ? '' : 'none';
    document.getElementById('formRegister').style.display = tab === 'register' ? '' : 'none';
    hideAuthError();
}

function showAuthError(msg) {
const el = document.getElementById('authError');
el.textContent = msg;
el.style.display = 'block';
}

function hideAuthError() {
    document.getElementById('authError').style.display = 'none';
}

async function doLogin() {
    hideAuthError();
    const user = document.getElementById('loginUser').value.trim();
    const pass = document.getElementById('loginPass').value;
    if (!user || !pass) { showAuthError('Inserisci username e password.'); return; }

    const res  = await fetch(SERVER + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user, password: pass })
    });
    const data = await res.json();

    if (!res.ok) { showAuthError(data.error || 'Errore login.'); return; }

    saveSession(data.token, data.username, data.credits);
    showApp();
    syncGameState();
}

async function doRegister() {
    hideAuthError();
    const user = document.getElementById('regUser').value.trim();
    const pass = document.getElementById('regPass').value;
    if (!user || !pass) { showAuthError('Inserisci username e password.'); return; }

    const res  = await fetch(SERVER + '/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user, password: pass })
    });
    const data = await res.json();

    if (!res.ok) { showAuthError(data.error || 'Errore registrazione.'); return; }

    saveSession(data.token, data.username, data.credits);
    showApp();
    syncGameState();
}

async function doLogout() {
    if (token) {
        await fetch(SERVER + '/api/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token }
        }).catch(() => {});
    }
    clearSession();
    showPage('page-auth');
}

function saveSession(t, u, c) {
    token    = t;
    username = u;
    credits  = c;
    sessionStorage.setItem('bj_token',    t);
    sessionStorage.setItem('bj_username', u);
    sessionStorage.setItem('bj_credits',  c);
}

function clearSession() {
    token = null; username = ''; credits = 0;
    sessionStorage.removeItem('bj_token');
    sessionStorage.removeItem('bj_username');
    sessionStorage.removeItem('bj_credits');
}

// ═══════════════════════════════════════════════════════════════════
//  NAVIGATION
// ═══════════════════════════════════════════════════════════════════
function showPage(id) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(id).classList.add('active');
}

function showApp() {
    showPage('page-app');
    updateNavbar();
}

function updateNavbar() {
    document.getElementById('navUsername').textContent = username;
    document.getElementById('navCredits').textContent  = Math.round(credits) + ' chips';
}

function showPanel(name) {
    document.querySelectorAll('.app-panel').forEach(p => p.classList.remove('active'));
    document.getElementById('panel-' + name).classList.add('active');
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    document.getElementById('nav' + name.charAt(0).toUpperCase() + name.slice(1))
            .classList.add('active');

    if (name === 'stats') loadStats();
}

// ═══════════════════════════════════════════════════════════════════
//  BETTING
// ═══════════════════════════════════════════════════════════════════
function changeBet(delta) {
    const newBet = currentBet + delta;
    if (newBet < 10 || newBet > 100) return;
    if (newBet > credits)            return;
    currentBet = newBet;
    document.getElementById('betDisplay').textContent = currentBet;
}

function setBetControlsEnabled(enabled) {
    document.getElementById('btnBetMinus').disabled = !enabled;
    document.getElementById('btnBetPlus').disabled  = !enabled;
}

// ═══════════════════════════════════════════════════════════════════
//  GAME BUTTONS
// ═══════════════════════════════════════════════════════════════════
function setButtonPhase(phase) {
    const start = document.getElementById('btnStart');
    const hit   = document.getElementById('btnHit');
    const stand = document.getElementById('btnStand');

    if (phase === 'playing') {
        start.classList.add('hidden');
        hit.classList.remove('hidden');
        stand.classList.remove('hidden');
        setBetControlsEnabled(false);
    } else {
        start.classList.remove('hidden');
        hit.classList.add('hidden');
        stand.classList.add('hidden');
        setBetControlsEnabled(true);
    }
}

// ═══════════════════════════════════════════════════════════════════
//  CARD RENDERING
// ═══════════════════════════════════════════════════════════════════
function attachGloss(cardEl) {
    if (cardEl.classList.contains('back')) return;
    cardEl.addEventListener('mousemove', e => {
        const r = cardEl.getBoundingClientRect();
        cardEl.style.setProperty('--x', ((e.clientX - r.left) / r.width  * 100) + '%');
        cardEl.style.setProperty('--y', ((e.clientY - r.top)  / r.height * 100) + '%');
    });
    cardEl.addEventListener('mouseleave', () => {
        cardEl.style.setProperty('--x', '50%');
        cardEl.style.setProperty('--y', '50%');
    });
}

function renderCards(containerId, imagePaths = [], labels = []) {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = '';

    if (!imagePaths.length) {
        const empty = document.createElement('div');
        empty.className = 'card';
        empty.textContent = '-';
        container.appendChild(empty);
        return;
    }

    imagePaths.forEach((src, i) => {
        const label  = labels[i] || '-';
        const isBack = label === 'Carta coperta' || src.includes('back_side');
        const card   = document.createElement('div');

        if (isBack) {
        card.className = 'card back';
        container.appendChild(card);
        return;
        }

        card.className = 'card';
        const img = document.createElement('img');
        img.src = src; img.alt = label; img.className = 'card-image';
        img.onerror = () => { card.textContent = ''; };
        card.appendChild(img);
        container.appendChild(card);
        attachGloss(card);
    });
}

// ═══════════════════════════════════════════════════════════════════
//  UI UPDATE
// ═══════════════════════════════════════════════════════════════════
function updateUI(state) {

    //console.log("updateUI chiamata:", state);

    renderCards('playerCardsArea', state.playerCardImages || [], state.playerCards || []);
    renderCards('dealerCardsArea', state.dealerCardImages || [], state.dealerCards || []);

    document.getElementById('playerValue').textContent = state.playerValue ?? 0;
    document.getElementById('dealerValue').textContent =
        state.dealerValueLabel ?? (state.gameOver ? (state.dealerValue ?? 0) : '?');

    document.getElementById('message').textContent =
        state.message || 'Nessun messaggio.';

    // Aggiorna crediti
    if (state.credits !== undefined) {
        credits = state.credits;
        sessionStorage.setItem('bj_credits', credits);
        updateNavbar();

        // Clamp la scommessa ai crediti disponibili
        if (currentBet > credits) {
        currentBet = Math.max(10, Math.floor(credits / 10) * 10);
        document.getElementById('betDisplay').textContent = currentBet;
        }
    }

    if (state.started && !state.gameOver) {
        setButtonPhase('playing');
    } else {
        setButtonPhase('idle');
    }
}

// ═══════════════════════════════════════════════════════════════════
//  API CALLS
// ═══════════════════════════════════════════════════════════════════
function authHeader() {
    return { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' };
    }

    async function syncGameState() {
    try {
        const res = await fetch(SERVER + '/api/game/state', { headers: authHeader() });
        if (!res.ok) { setButtonPhase('idle'); return; }
        const data = await res.json();
        updateUI(data);
    } catch { setButtonPhase('idle'); }
}

async function startGame() {
    if (currentBet < 10) {
        document.getElementById('message').textContent = 'Imposta una scommessa prima di iniziare.';
        return;
    }
    const res  = await fetch(SERVER + '/api/game/start', {
        method: 'POST',
        headers: authHeader(),
        body: JSON.stringify({ bet: currentBet })
    });
    const data = await res.json();
    if (!res.ok) {
        document.getElementById('message').textContent = data.error || 'Errore.';
        return;
    }
    updateUI(data);
}

async function hit() {
    const res  = await fetch(SERVER + '/api/game/hit', { method: 'POST', headers: authHeader() });
    const data = await res.json();
    updateUI(data);
}

async function stand() {
    const res  = await fetch(SERVER + '/api/game/stand', { method: 'POST', headers: authHeader() });
    const data = await res.json();
    updateUI(data);
}

// ═══════════════════════════════════════════════════════════════════
//  STATS
// ═══════════════════════════════════════════════════════════════════
async function loadStats() {
    document.getElementById('statsContent').innerHTML =
        '<div class="stats-loading">Caricamento...</div>';

    try {
        const res  = await fetch(SERVER + '/api/stats', { headers: authHeader() });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);
        renderStats(data);
    } catch (e) {
        document.getElementById('statsContent').innerHTML =
        '<div class="stats-loading">Errore caricamento statistiche.</div>';
    }
}

function renderStats(d) {
const winRate = d.totale > 0 ? Math.round(d.vittorie / d.totale * 100) : 0;

const rows = (d.ultimePartite || []).map(p => `
    <tr>
    <td>${p.data.replace('T', ' ')}</td>
    <td><span class="esito-badge esito-${p.esito}">${p.esito}</span></td>
    <td>${p.punti}</td>
    <td>${p.scommessa > 0 ? p.scommessa + ' chips' : '—'}</td>
    </tr>
`).join('');

document.getElementById('statsContent').innerHTML = `
    <div class="stats-grid">
    <div class="stat-card">
        <div class="stat-value">${Math.round(d.crediti)}</div>
        <div class="stat-label">Chips totali</div>
    </div>
    <div class="stat-card">
        <div class="stat-value">${d.totale}</div>
        <div class="stat-label">Partite giocate</div>
    </div>
    <div class="stat-card">
        <div class="stat-value" style="color:#2ec878">${d.vittorie}</div>
        <div class="stat-label">Vittorie</div>
    </div>
    <div class="stat-card">
        <div class="stat-value" style="color:#e05555">${d.sconfitte}</div>
        <div class="stat-label">Sconfitte</div>
    </div>
    <div class="stat-card">
        <div class="stat-value" style="color:#8090e0">${d.pareggi}</div>
        <div class="stat-label">Pareggi</div>
    </div>
    <div class="stat-card">
        <div class="stat-value">${winRate}%</div>
        <div class="stat-label">Win rate</div>
    </div>
    </div>

    <div class="stats-section-title">Ultime partite</div>
    <div class="zone" style="padding:0;overflow:hidden">
    ${rows.length ? `
        <div class="history-table-wrap">
            <table class="history-table">
                <thead>
                    <tr>
                        <th>Data</th>
                        <th>Esito</th>
                        <th>Punti</th>
                        <th>Scommessa</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>` :
        '<div style="padding:24px;text-align:center;color:var(--text-dim)">Nessuna partita ancora giocata.</div>'
    }
    </div>`;
}

// ═══════════════════════════════════════════════════════════════════
//  ABANDON ON UNLOAD
// ═══════════════════════════════════════════════════════════════════
window.addEventListener('beforeunload', () => {
if (token) navigator.sendBeacon(SERVER + '/api/game/abandon',
    new Blob([JSON.stringify({ token })], { type: 'application/json' }));
});