@echo off
set PYTHON_PATH=C:\Users\Administrator\AppData\Local\Programs\Python\Python313\python.exe
set SCRIPT_PATH=C:\monitor\monitoramento_backup.py

echo Iniciando script de monitoramento...
%PYTHON_PATH% "%SCRIPT_PATH%"
echo Finalizado em %DATE% %TIME%
