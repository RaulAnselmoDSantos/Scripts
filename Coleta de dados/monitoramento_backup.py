#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import csv
import datetime
import sqlite3
from pathlib import Path

# === CONFIGURA��O ===
BACKUP_ROOT = Path(r"E:\BACKUPS")                               # raiz das pastas de cliente
CSV_HIST    = Path(r"C:\monitor\backup_sizes_history.csv")      # CSV de hist�rico
DB_PATH     = Path(r"C:\monitor\backup_status.db")              # banco de dados SQLite

# === ETAPA 1: Verificacao de tamanho das pastas e escrita no CSV ===

now = datetime.datetime.now().isoformat(timespec="seconds")
first_time = not CSV_HIST.exists()
CSV_HIST.parent.mkdir(parents=True, exist_ok=True)

print(f"[INFO] Iniciando coleta de tamanhos em {BACKUP_ROOT}...")

with CSV_HIST.open("a", newline="", encoding="utf-8") as f:
    writer = csv.writer(f)
    if first_time:
        writer.writerow(["client", "ts_verificacao", "size_mb"])

    for client_dir in BACKUP_ROOT.iterdir():
        if not client_dir.is_dir():
            continue

        total_bytes = 0
        for file in client_dir.rglob("*"):
            if file.is_file():
                try:
                    total_bytes += file.stat().st_size
                except OSError:
                    pass

        size_mb = round(total_bytes / (1024 * 1024), 2)
        writer.writerow([client_dir.name, now, size_mb])
        print(f"[OK] {client_dir.name} - {size_mb} MB")

print(f"[OK] CSV atualizado: {CSV_HIST}")

# === ETAPA 2: Atualizar banco de dados SQLite com os dados do CSV ===

print("[INFO] Atualizando banco de dados...")

conn = sqlite3.connect(DB_PATH)
conn.execute("PRAGMA journal_mode=WAL;")
cur = conn.cursor()

# 1) Criacao das tabelas
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

# 2) Importar novas linhas do CSV
with CSV_HIST.open(newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    all_rows = [(r["client"], r["ts_verificacao"], float(r["size_mb"])) for r in reader]

cur.execute("SELECT MAX(ts_verificacao) FROM history_backups")
max_ts = cur.fetchone()[0]
new_rows = [r for r in all_rows if max_ts is None or r[1] > max_ts]

if new_rows:
    cur.executemany(
        "INSERT INTO history_backups(cliente, ts_verificacao, size_mb) VALUES (?,?,?);",
        new_rows
    )
    conn.commit()

# 3) Atualizar status_backups
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
    last_change = ts_now if diferenca != 0 else None

    cur.execute(upsert_sql,
        (client, ts_prev, size_prev, ts_now, size_now, diferenca, last_change)
    )

conn.commit()
conn.close()
print(f"[OK] status_backups atualizado com {len(clients)} clientes.")
