# -*- coding: cp1252 -*-

#!/usr/bin/env python3
import csv
import sqlite3
from pathlib import Path

# === CONFIGURAÇÃO ===
CSV_HIST = Path(r"C:\monitor\backup_sizes_history.csv")
DB_PATH  = Path(r"C:\monitor\backup_status.db")

# conecta (mantendo o arquivo entre execuções) e habilita WAL
conn = sqlite3.connect(DB_PATH)
conn.execute("PRAGMA journal_mode=WAL;")
cur = conn.cursor()

# 1) cria as tabelas se não existirem
cur.executescript("""
CREATE TABLE IF NOT EXISTS history_backups (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  cliente        TEXT,
  ts_verificacao TEXT,
  size_mb        REAL
);
CREATE INDEX IF NOT EXISTS idx_hist_client_ts
  ON history_backups(cliente, ts_verificacao);

CREATE TABLE IF NOT EXISTS status_backups (
  cliente                  TEXT PRIMARY KEY,
  ts_anterior_verificacao  TEXT,
  size_anterior_mb         REAL,
  ts_atual_verificacao     TEXT,
  size_atual_mb            REAL,
  diferenca_mb             REAL,
  last_change_ts           TEXT
);
""")
conn.commit()

# 2) importa novas linhas do CSV histórico
with CSV_HIST.open(newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    all_rows = [(r["client"], r["ts_verificacao"], float(r["size_mb"])) for r in reader]

# pega o maior ts já importado (evita duplicar)
cur.execute("SELECT MAX(ts_verificacao) FROM history_backups")
max_ts = cur.fetchone()[0]
new_rows = [r for r in all_rows if max_ts is None or r[1] > max_ts]

if new_rows:
    cur.executemany(
        "INSERT INTO history_backups(cliente, ts_verificacao, size_mb) VALUES (?,?,?);",
        new_rows
    )
    conn.commit()

# 3) para cada cliente, busca as 2 últimas verificações e upserta em status_backups
cur.execute("SELECT DISTINCT cliente FROM history_backups")
clients = [row[0] for row in cur.fetchall()]

upsert_sql = """
INSERT INTO status_backups(
    cliente,
    ts_anterior_verificacao,
    size_anterior_mb,
    ts_atual_verificacao,
    size_atual_mb,
    diferenca_mb,
    last_change_ts
) VALUES (?,?,?,?,?,?,?)
ON CONFLICT(cliente) DO UPDATE SET
    ts_anterior_verificacao = excluded.ts_anterior_verificacao,
    size_anterior_mb        = excluded.size_anterior_mb,
    ts_atual_verificacao    = excluded.ts_atual_verificacao,
    size_atual_mb           = excluded.size_atual_mb,
    diferenca_mb            = excluded.diferenca_mb,
    last_change_ts          = COALESCE(excluded.last_change_ts, status_backups.last_change_ts);
"""

for client in clients:
    cur.execute("""
      SELECT ts_verificacao, size_mb
        FROM history_backups
       WHERE cliente = ?
    ORDER BY ts_verificacao DESC
       LIMIT 2
    """, (client,))
    rows = cur.fetchall()
    if not rows:
        continue

    ts_now, size_now = rows[0]
    if len(rows) > 1:
        ts_prev, size_prev = rows[1]
    else:
        ts_prev, size_prev = None, 0.0

    diferenca = size_now - size_prev
    # atualiza last_change_ts só se houve mudança
    last_change = ts_now if diferenca != 0 else None

    cur.execute(upsert_sql,
        (client, ts_prev, size_prev, ts_now, size_now, diferenca, last_change)
    )

conn.commit()
conn.close()
print(f"[OK] status_backups atualizado com {len(clients)} clientes.")

