# Backup & Automation Scripts

Bem-vindo ao repositório **Backup & Automation Scripts**! Este repositório contém scripts desenvolvidos para automatizar tarefas de backup, gerenciamento de usuários, configuração de dispositivos MikroTik e processamento de logs.

## Estrutura do Repositório

### 1. **Java (Interface)**
Scripts relacionados a funcionalidades de interfaces específicas desenvolvidas em Java:
- `BackupUserManager.java`
- `BackupUserManagerGUI.java`
- `BackupUserManagerInteractive.java`
- `BackupUserManagerInterface.java`
- `BackupUserManagerV2.java`
- `BackupUserManagerV3.java`

Esses arquivos demonstram técnicas de gerenciamento de backups e usuários utilizando Java.

### 2. **MikroTik**
Scripts e configurações para dispositivos MikroTik:
- `backup-mikrotik-01.backup`
- `FB_OK_VEL_OK_2.backup`
- `full-config.rsc`
- `M3uM1kr0t1c.txt`
- `routeros-7.16.2-mmips.npk`
- `SCRIPT FAILOVER - ROUTEROS V6.txt`

Automatização de configurações e recuperação de backups.

### 3. **Scripts em Shell e Python**
Scripts para automação e processamento de logs:
- **Powershell:**
  - `scriptUnificado.ps1`
  - `scriptUnificadoV2.ps1`
  - `scriptUnificadoV3.ps1`
  - `scriptUnificadoV4.ps1`
  - `scriptUnificadoV5 DISCO E.ps1`
- **Python:**
  - `processa_logs_firewall.py`

Scripts focados em tarefas de automação e processamento, incluindo análise de logs de firewall.

---

## Requisitos

Certifique-se de que você possui os seguintes softwares instalados:

- **Java Development Kit (JDK)** para scripts Java.
- **Python 3.8+** para executar o script de logs.
- **Powershell** para os scripts do Windows.
- **RouterOS** compatível para os scripts MikroTik.

---

## Como Utilizar

### Scripts Java
1. Compile os arquivos `.java` utilizando um compilador Java (exemplo: `javac`).
2. Execute o programa principal (exemplo: `java BackupUserManager`).

### Scripts MikroTik
1. Faça upload dos arquivos `.backup` ou `.rsc` para o dispositivo MikroTik.
2. Execute os comandos ou scripts via terminal ou interface gráfica.

### Scripts Shell e Python
1. Certifique-se de que possui as permissões adequadas para execução.
2. Execute os scripts Powershell diretamente no terminal:
   ```powershell
   .\scriptUnificado.ps1
   ```
3. Para Python:
   ```bash
   python processa_logs_firewall.py
   ```

---

## Contribuição

Contribuições são bem-vindas! Sinta-se à vontade para abrir issues e pull requests para melhorias.

---

## Licença

Este projeto está licenciado sob a [MIT License](LICENSE).

---

## Contato

Para dúvidas ou sugestões, entre em contato:

- **Autor:** Raul Anselmo
- **Email:** raul.trabalho5511@gmail.com
```

