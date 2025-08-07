#!/usr/bin/env python3
import os
import csv
import datetime
from pathlib import Path

# === CONFIGURAÇÃO ===
BACKUP_ROOT = Path(r"E:\BACKUPS")                               # raiz das pastas de cliente
HIST_CSV    = Path(r"C:\monitor\backup_sizes_history.csv")      # CSV de histórico

# timestamp com segundos
now = datetime.datetime.now().isoformat(timespec="seconds")

# cria o CSV (com header só na 1ª vez) em modo append
first_time = not HIST_CSV.exists()
HIST_CSV.parent.mkdir(parents=True, exist_ok=True)

with HIST_CSV.open("a", newline="", encoding="utf-8") as f:
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

print(f"[OK] Histórico atualizado em {HIST_CSV}")
