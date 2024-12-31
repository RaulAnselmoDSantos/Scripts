$backupPath = "D:\BACKUPS\"
$logFile = "C:\Scripts\logs\logfile.txt"
$usuariosFile = "C:\Scripts\utilizados\usuarios_bkp.txt"
$grupo = "F1SFTP"

# Função para registrar logs no arquivo
function Registrar-Log {
    param ([string]$mensagem)
    Add-Content -Path $logFile -Value "$((Get-Date -Format 'yyyy-MM-dd HH:mm:ss')) - $mensagem"
}

Registrar-Log "Iniciando o script para criação de usuários."

# Verificar se o grupo F1SFTP existe, se não existir, cria o grupo
if (-not (Get-LocalGroup | Where-Object Name -eq $grupo)) {
    New-LocalGroup -Name $grupo -Description "Grupo para usuários do SFTP"
    Registrar-Log "Grupo $grupo criado."
} else {
    Registrar-Log "Grupo $grupo já existe."
}

# Ler a lista de usuários e senhas do arquivo
try {
    Write-Host "Lendo o arquivo de usuários: $usuariosFile"
    Registrar-Log "Lendo o arquivo de usuários: $usuariosFile"
    
    $usuarios = Get-Content $usuariosFile | ForEach-Object {
        # Exibir cada linha conforme é lida para confirmação
        Write-Host "Lendo linha: $_"
        Registrar-Log "Lendo linha: $_"
        
        # Dividir a linha e verificar se há exatamente duas partes (usuário e senha)
        $fields = $_ -split ","
        if ($fields.Length -eq 2) {
            Write-Host "Usuário: $($fields[0].Trim()), Senha: $($fields[1].Trim())"
            [PSCustomObject]@{ 
                Usuario = $fields[0].Trim()
                Senha   = $fields[1].Trim()
            }
        } else {
            Write-Host "Erro: Formato incorreto na linha: $_"
            Registrar-Log "Erro: Formato incorreto na linha: $_"
            $null  # Ignorar a linha com erro de formato
        }
    } | Where-Object { $_ -ne $null }  # Filtrar linhas inválidas
    
    # Confirmar se algum usuário foi carregado
    if ($usuarios.Count -eq 0) {
        Write-Host "Nenhum usuário válido encontrado no arquivo. Saindo."
        Registrar-Log "Nenhum usuário válido encontrado no arquivo. Saindo."
        exit
    }
    Registrar-Log "Arquivo de usuários e senhas lido com sucesso."
    Write-Host "Arquivo de usuários e senhas lido com sucesso."
} catch {
    Registrar-Log "Erro ao ler o arquivo de usuários e senhas: $_"
    Write-Host "Erro ao ler o arquivo de usuários e senhas: $_"
    exit
}

# Loop para criar usuários, criar a pasta de backup e configurá-los
foreach ($usuarioObj in $usuarios) {
    $usuario = $usuarioObj.Usuario
    $senha = $usuarioObj.Senha

    try {
        # Verificar se o nome do usuário tem mais de 20 caracteres
        if ($usuario.Length -gt 20) {
            Registrar-Log "Erro: O nome do usuário $usuario excede o limite de 20 caracteres."
            Write-Host "Erro: O nome do usuário $usuario excede o limite de 20 caracteres."
            continue
        }

        # Verificar se o usuário já existe
        if (Get-LocalUser | Where-Object Name -eq $usuario) {
            Registrar-Log "Usuário $usuario já existe. Pulando a criação."
            Write-Host "Usuário $usuario já existe. Pulando a criação."
        } else {
            # Criar o usuário com a senha correspondente
            $senhaSegura = ConvertTo-SecureString $senha -AsPlainText -Force
            New-LocalUser -Name $usuario -Password $senhaSegura -PasswordNeverExpires -AccountNeverExpires
            Registrar-Log "Usuário $usuario criado com sucesso."
            Write-Host "Usuário $usuario criado com sucesso."

            # Adicionar o usuário ao grupo F1SFTP
            Add-LocalGroupMember -Group $grupo -Member $usuario
            Registrar-Log "Usuário $usuario adicionado ao grupo $grupo."
            Write-Host "Usuário $usuario adicionado ao grupo $grupo."
        }

        # Criar o diretório de backup para o usuário
        $userDir = Join-Path -Path $backupPath -ChildPath $usuario

        # Verificar se a pasta já existe antes de criar
        if (-not (Test-Path -Path $userDir)) {
            New-Item -Path $userDir -ItemType Directory
            Registrar-Log "Diretório de backup $userDir criado com sucesso."
            Write-Host "Diretório de backup $userDir criado com sucesso."
        } else {
            Registrar-Log "Diretório de backup $userDir já existe. Pulando a criação."
            Write-Host "Diretório de backup $userDir já existe. Pulando a criação."
        }

        # Definir permissões específicas para o diretório do usuário
        # O diretório raiz do chroot deve ser de propriedade do Administrators
        $acl = Get-Acl $userDir
        $adminGroup = "BUILTIN\Administrators"
        $userPermission = New-Object System.Security.AccessControl.FileSystemAccessRule($usuario, "Modify", "ContainerInherit, ObjectInherit", "None", "Allow")
        
        # Adicionar permissões de administrador e do usuário
        $acl.SetOwner([System.Security.Principal.NTAccount]$adminGroup)
        $acl.SetAccessRule($userPermission)
        Set-Acl $userDir $acl
        Registrar-Log "Permissões configuradas para o diretório do usuário $usuario."
        Write-Host "Permissões configuradas para o diretório do usuário $usuario."

    } catch {
        Registrar-Log "Erro ao criar/adicionar o usuário $usuario : $_"
        Write-Host "Erro ao criar/adicionar o usuário $usuario : $_"
    }
}

# Finalizar log
Registrar-Log "Processo de criação de usuários e configuração de permissões finalizado."
Write-Host "Script finalizado. Pressione Enter para fechar."
Read-Host
