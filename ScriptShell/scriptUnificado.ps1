# Definir o caminho base para o diretório de backups, o log, e o arquivo de usuários
$backupPath = "D:\BACKUPS\"
$logFile = "C:\Users\Raul\Desktop\Configuração Backup\scripts\logs\logfile.txt"
$usuariosFile = "C:\Users\Raul\Desktop\Configuração Backup\scripts\logs\usuarios_bkp.txt"
$grupo = "F1SFTP"

# Função para registrar logs no arquivo
function Registrar-Log {
    param ([string]$mensagem)
    Add-Content -Path $logFile -Value "$((Get-Date -Format 'yyyy-MM-dd HH:mm:ss')) - $mensagem"
}

# Função para verificar se o usuário existe
function Verificar-Usuario {
    param([string]$nome)
    try {
        $null = Get-LocalUser -Name $nome -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

# Função para verificar se o grupo existe
function Verificar-Grupo {
    param([string]$nome)
    try {
        $null = Get-LocalGroup -Name $nome -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

# Iniciar log
Registrar-Log "Iniciando a criação de usuários e configuração de permissões."

# Verificar se o grupo F1SFTP existe, se não existir, cria o grupo
if (-not (Verificar-Grupo $grupo)) {
    New-LocalGroup -Name $grupo -Description "Grupo para usuários do SFTP"
    Registrar-Log "Grupo $grupo criado."
} else {
    Registrar-Log "Grupo $grupo já existe."
}

# Ler a lista de usuários e senhas do arquivo
try {
    $usuarios = Get-Content $usuariosFile | ForEach-Object {
        $fields = $_ -split ","
        [PSCustomObject]@{ 
            Usuario = $fields[0]
            Senha   = $fields[1]
        }
    }
    Registrar-Log "Arquivo de usuários e senhas lido com sucesso."
} catch {
    Registrar-Log "Erro ao ler o arquivo de usuários e senhas: $_"
    Read-Host "Erro ao ler o arquivo de usuários. Pressione Enter para fechar"
    exit
}

# Definir permissões no diretório raiz do chroot
$acl = Get-Acl $backupPath
$adminGroup = "Administrators"
$adminPermission = New-Object System.Security.AccessControl.FileSystemAccessRule($adminGroup, "FullControl", "ContainerInherit, ObjectInherit", "None", "Allow")
$noWritePermission = New-Object System.Security.AccessControl.FileSystemAccessRule($grupo, "ReadAndExecute", "ContainerInherit, ObjectInherit", "None", "Allow")
$acl.SetAccessRule($adminPermission)
$acl.SetAccessRule($noWritePermission)
Set-Acl $backupPath $acl

Write-Host "Permissões configuradas para o diretório raiz $backupPath."
Registrar-Log "Permissões configuradas para o diretório raiz $backupPath."

# Loop para criar usuários, criar a pasta de backup e configurá-los
foreach ($usuarioObj in $usuarios) {
    $usuario = $usuarioObj.Usuario
    $senha = $usuarioObj.Senha

    try {
        # Verificar se o nome do usuário tem mais de 20 caracteres
        if ($usuario.Length -gt 20) {
            Registrar-Log "Erro: O nome do usuário $usuario excede o limite de 20 caracteres."
            continue
        }

        # Verificar se o usuário já existe
        if (Get-LocalUser -Name $usuario -ErrorAction SilentlyContinue) {
            Registrar-Log "Usuário $usuario já existe. Pulando a criação."
        } else {
            # Criar o usuário com a senha correspondente
            $senhaSegura = ConvertTo-SecureString $senha -AsPlainText -Force
            New-LocalUser -Name $usuario -Password $senhaSegura -PasswordNeverExpires -AccountNeverExpires
            Registrar-Log "Usuário $usuario criado com sucesso."

            # Adicionar o usuário ao grupo F1SFTP
            Add-LocalGroupMember -Group $grupo -Member $usuario
            Registrar-Log "Usuário $usuario adicionado ao grupo $grupo."
        }

        # Criar o diretório de backup para o usuário
        $userDir = Join-Path -Path $backupPath -ChildPath $usuario

        # Verificar se a pasta já existe antes de criar
        if (-not (Test-Path -Path $userDir)) {
            New-Item -Path $userDir -ItemType Directory
            Registrar-Log "Diretório de backup $userDir criado com sucesso."
        } else {
            Registrar-Log "Diretório de backup $userDir já existe. Pulando a criação."
        }

        # Definir permissões específicas para o diretório do usuário
        $acl = Get-Acl $userDir
        $userPermission = New-Object System.Security.AccessControl.FileSystemAccessRule($usuario, "Modify", "ContainerInherit, ObjectInherit", "None", "Allow")
        $acl.SetAccessRule($userPermission)
        Set-Acl $userDir $acl
        Write-Host "Permissões configuradas para o diretório do usuário $usuario."
        Registrar-Log "Permissões configuradas para o diretório do usuário $usuario."

    } catch {
        Registrar-Log "Erro ao criar/adicionar o usuário $usuario : $_"
    }
}

# Pausa para evitar fechamento abrupto
Read-Host "Script finalizado. Pressione Enter para fechar."

# Finalizar log
Registrar-Log "Processo de criação de usuários e configuração de permissões finalizado."
