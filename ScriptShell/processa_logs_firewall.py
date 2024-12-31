import re
from collections import defaultdict

def processar_logs(arquivo_log):
    # Padrões de Regex
    padrao_sucesso = re.compile(r"(\d{4}-\d{2}-\d{2}) \d{2}:\d{2}:\d{2}\.\d+ Accepted password for (\S+) from (\d+\.\d+\.\d+\.\d+) port \d+")
    padrao_invalido = re.compile(r"Invalid user (\S+) from (\d+\.\d+\.\d+\.\d+)")
    padrao_falha = re.compile(r"Failed password for invalid user (\S+) from (\d+\.\d+\.\d+\.\d+)")

    # Dicionários para armazenar resultados
    acessos_sucesso = defaultdict(lambda: defaultdict(lambda: {"count": 0, "data": None}))
    acessos_invalidos = defaultdict(int)
    ips_sucesso = set()
    ips_falha = set()

    # Processamento do arquivo
    with open(arquivo_log, 'r') as arquivo:
        for linha in arquivo:
            # Match para acessos bem-sucedidos
            match_sucesso = padrao_sucesso.search(linha)
            if match_sucesso:
                data, usuario, ip = match_sucesso.groups()
                if acessos_sucesso[usuario][ip]["data"] is None or data < acessos_sucesso[usuario][ip]["data"]:
                    acessos_sucesso[usuario][ip]["data"] = data
                acessos_sucesso[usuario][ip]["count"] += 1
                ips_sucesso.add(ip)

            # Match para acessos inválidos
            match_invalido = padrao_invalido.search(linha)
            if match_invalido:
                usuario, ip = match_invalido.groups()
                acessos_invalidos[(usuario, ip)] += 1
                ips_falha.add(ip)

            # Match para falhas de senha
            match_falha = padrao_falha.search(linha)
            if match_falha:
                usuario, ip = match_falha.groups()
                acessos_invalidos[(usuario, ip)] += 1
                ips_falha.add(ip)

    # Remover IPs com acesso bem-sucedido da lista de falhas
    ips_falha -= ips_sucesso

    # Resumo de Acessos Bem-Sucedidos
    with open("resumo_acessos.txt", "w") as resumo_file:
        resumo_file.write("===== Resumo de Acessos Bem-Sucedidos =====\n\n")
        for usuario, ips in acessos_sucesso.items():
            resumo_file.write(f"Usuário: {usuario}\n")
            for ip, info in ips.items():
                resumo_file.write(f"[{info['data']}]    IP: {ip}, Total de logins: {info['count']}\n")
            resumo_file.write("\n")

    # Lista de IPs com falhas
    with open("ips_falha.txt", "w") as falha_file:
        falha_file.writelines(f"{ip}\n" for ip in sorted(ips_falha))

    # Análise de IPs com falha que conseguiram logar
    with open("analise_brute_force.txt", "w") as brute_file:
        brute_file.write("===== Análise de IPs Possíveis Brute-Force =====\n\n")
        for ip in sorted(ips_falha & ips_sucesso):
            brute_file.write(f"IP: {ip} - Falhou e posteriormente conseguiu logar\n")

    print("Análise concluída. Arquivos gerados:")
    print("- 'resumo_acessos.txt': Detalhes dos logins bem-sucedidos (incluindo data do primeiro login).")
    print("- 'ips_falha.txt': Lista de IPs com falhas.")
    print("- 'analise_brute_force.txt': IPs que falharam e conseguiram acessar depois.")

# Caminho do arquivo de log
arquivo_log = "arquivos_log.log"  # Substitua pelo caminho do seu arquivo de log

# Execução
processar_logs(arquivo_log)
