const express = require('express');
const cors    = require('cors');
const fs      = require('fs');
const path    = require('path');
const { execSync } = require('child_process');

const app  = express();
const PORT = 3030;

const CONFIG_PATH          = path.join(__dirname, 'config.json');
const SERVICE_ACCOUNT_PATH = path.join(__dirname, 'service-account.json');
const ASSETS_DIR           = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'content');
const COL                  = 'app_content';

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// ── Firebase Admin (optional — only if service-account.json exists) ────
let firestoreDb = null;
if (fs.existsSync(SERVICE_ACCOUNT_PATH)) {
  try {
    const admin = require('firebase-admin');
    const serviceAccount = JSON.parse(fs.readFileSync(SERVICE_ACCOUNT_PATH, 'utf8'));
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    firestoreDb = admin.firestore();
    console.log('✅ Firebase Admin מחובר ל-Firestore');
  } catch (e) {
    console.warn('⚠️  Firebase Admin נכשל:', e.message);
  }
} else {
  console.log('ℹ️  service-account.json לא נמצא — עובד במצב מקומי בלבד');
}

// ── Firestore helpers ─────────────────────────────────────────────────
async function fsGet(docName) {
  if (!firestoreDb) return null;
  const snap = await firestoreDb.collection(COL).doc(docName).get();
  return snap.exists ? snap.data() : null;
}

async function fsSet(docName, data) {
  if (!firestoreDb) return false;
  await firestoreDb.collection(COL).doc(docName).set(data, { merge: true });
  return true;
}

// ── API: Firestore status ─────────────────────────────────────────────
app.get('/api/firestore/status', (req, res) => {
  res.json({ connected: !!firestoreDb });
});

// ── API: read / write Firestore document ─────────────────────────────
app.get('/api/firestore/:doc', async (req, res) => {
  try {
    const data = await fsGet(req.params.doc);
    res.json(data || {});
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.post('/api/firestore/:doc', async (req, res) => {
  try {
    const ok = await fsSet(req.params.doc, req.body);
    res.json({ ok, saved: ok ? 'firestore' : 'local-only' });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── API: seed Firestore from local JSON assets ────────────────────────
app.post('/api/firestore/seed', async (req, res) => {
  if (!firestoreDb) return res.status(503).json({ error: 'Firestore לא מחובר' });
  try {
    const blessings  = JSON.parse(fs.readFileSync(path.join(ASSETS_DIR, 'blessings.json'), 'utf8'));
    const prayers    = JSON.parse(fs.readFileSync(path.join(ASSETS_DIR, 'special_prayers.json'), 'utf8'));
    const shalom     = JSON.parse(fs.readFileSync(path.join(ASSETS_DIR, 'shalom_bayit.json'), 'utf8'));
    const challa     = JSON.parse(fs.readFileSync(path.join(ASSETS_DIR, 'challa.json'), 'utf8'));
    const tracking   = JSON.parse(fs.readFileSync(path.join(ASSETS_DIR, 'spiritual_tracking.json'), 'utf8'));

    await firestoreDb.collection(COL).doc('blessings').set({ items: blessings });
    await firestoreDb.collection(COL).doc('special_prayers').set({ items: prayers });
    await firestoreDb.collection(COL).doc('shalom_bayit_tips').set({ items: shalom.tips || [] });
    await firestoreDb.collection(COL).doc('shalom_bayit_verses').set({ verses: shalom.verses || [] });
    await firestoreDb.collection(COL).doc('challa_steps').set({ items: challa.steps || [] });
    await firestoreDb.collection(COL).doc('challa_recipes').set({ items: challa.recipes || [] });
    await firestoreDb.collection(COL).doc('spiritual_tracking').set({ items: tracking });
    await firestoreDb.collection(COL).doc('daily_messages').set({ items: [] });
    await firestoreDb.collection(COL).doc('bot_welcome').set({ text: '' });

    res.json({ ok: true, message: '9 מסמכים הועלו ל-Firestore בהצלחה' });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

// ── API: local config (tool visibility + theme) ───────────────────────
app.get('/api/config', (req, res) => {
  try { res.json(JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'))); }
  catch (e) { res.status(500).json({ error: e.message }); }
});

app.post('/api/config', (req, res) => {
  try { fs.writeFileSync(CONFIG_PATH, JSON.stringify(req.body, null, 2)); res.json({ ok: true }); }
  catch (e) { res.status(500).json({ error: e.message }); }
});

// ── API: read / write asset JSON ──────────────────────────────────────
app.get('/api/assets/:file', (req, res) => {
  try {
    const data = fs.readFileSync(path.join(ASSETS_DIR, req.params.file), 'utf8');
    res.json(JSON.parse(data));
  } catch (e) { res.status(404).json({ error: e.message }); }
});

app.post('/api/assets/:file', (req, res) => {
  try {
    fs.writeFileSync(path.join(ASSETS_DIR, req.params.file), JSON.stringify(req.body, null, 2));
    res.json({ ok: true });
  } catch (e) { res.status(500).json({ error: e.message }); }
});

// ── API: git ──────────────────────────────────────────────────────────
app.get('/api/git-status', (req, res) => {
  try {
    const root   = path.join(__dirname, '..');
    const branch = execSync(`git -C "${root}" branch --show-current`, { stdio: 'pipe' }).toString().trim();
    const status = execSync(`git -C "${root}" status --short`,        { stdio: 'pipe' }).toString().trim();
    res.json({ branch, status: status || 'clean' });
  } catch (e) { res.json({ branch: 'unknown', status: e.message }); }
});

app.post('/api/git-push', (req, res) => {
  try {
    const root = path.join(__dirname, '..');
    const msg  = (req.body.message || 'admin: update configuration').replace(/"/g, "'");
    execSync(`git -C "${root}" add admin/config.json app/src/main/assets/content/`, { stdio: 'pipe' });
    execSync(`git -C "${root}" commit -m "${msg}"`, { stdio: 'pipe' });
    execSync(`git -C "${root}" push`, { stdio: 'pipe' });
    res.json({ ok: true, message: 'נדחף ל-Git בהצלחה' });
  } catch (e) {
    const m = e.stderr?.toString() || e.message;
    res.json({ ok: m.includes('nothing to commit'), message: m.includes('nothing to commit') ? 'אין שינויים חדשים' : m });
  }
});

app.listen(PORT, () => {
  console.log(`\n🕍 מערכת ניהול: http://localhost:${PORT}\n`);
});
