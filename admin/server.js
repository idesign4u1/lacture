const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const app = express();
const PORT = 3030;
const CONFIG_PATH = path.join(__dirname, 'config.json');

app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// ── Read config ───────────────────────────────────────────────────────
app.get('/api/config', (req, res) => {
  try {
    const data = fs.readFileSync(CONFIG_PATH, 'utf8');
    res.json(JSON.parse(data));
  } catch (e) {
    res.status(500).json({ error: 'Could not read config: ' + e.message });
  }
});

// ── Save config ───────────────────────────────────────────────────────
app.post('/api/config', (req, res) => {
  try {
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(req.body, null, 2), 'utf8');
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ error: 'Could not save config: ' + e.message });
  }
});

// ── Git commit & push ─────────────────────────────────────────────────
app.post('/api/git-push', (req, res) => {
  try {
    const repoRoot = path.join(__dirname, '..');
    const message = req.body.message || 'admin: update app configuration';
    execSync(`git -C "${repoRoot}" add admin/config.json`, { stdio: 'pipe' });
    execSync(`git -C "${repoRoot}" commit -m "${message}"`, { stdio: 'pipe' });
    execSync(`git -C "${repoRoot}" push`, { stdio: 'pipe' });
    res.json({ ok: true, message: 'השינויים נשמרו ונדחפו ל-Git בהצלחה' });
  } catch (e) {
    const msg = e.stderr?.toString() || e.message;
    // If nothing to commit, still OK
    if (msg.includes('nothing to commit')) {
      res.json({ ok: true, message: 'אין שינויים חדשים לדחוף' });
    } else {
      res.status(500).json({ error: msg });
    }
  }
});

// ── Git status ────────────────────────────────────────────────────────
app.get('/api/git-status', (req, res) => {
  try {
    const repoRoot = path.join(__dirname, '..');
    const branch = execSync(`git -C "${repoRoot}" branch --show-current`, { stdio: 'pipe' }).toString().trim();
    const status = execSync(`git -C "${repoRoot}" status --short`, { stdio: 'pipe' }).toString().trim();
    res.json({ branch, status: status || 'clean' });
  } catch (e) {
    res.json({ branch: 'unknown', status: e.message });
  }
});

app.listen(PORT, () => {
  console.log(`\n✅ מערכת הניהול פועלת בכתובת: http://localhost:${PORT}\n`);
});
