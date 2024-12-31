import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.List;
import java.util.Scanner;

public class BackupUserManagerInteractive {

    private static final String BACKUP_PATH = "E:\\BACKUPS\\";
    private static final String LOG_FILE = "C:\\Scripts\\logs\\logfile_interactive.txt";
    private static final String GROUP_NAME = "F1SFTP2";

    public static void main(String[] args) {
        System.out.println("Iniciando o script interativo para criação de usuário e diretório.");
        log("Iniciando o script interativo para criação de usuário e diretório.");

        try (Scanner scanner = new Scanner(System.in)) {
            // Verificar e criar grupo
            System.out.println("Verificando se o grupo " + GROUP_NAME + " existe.");
            log("Verificando se o grupo " + GROUP_NAME + " existe.");
            if (!isGroupExists(GROUP_NAME)) {
                if (createGroup(GROUP_NAME)) {
                    System.out.println("Grupo " + GROUP_NAME + " criado com sucesso.");
                    log("Grupo " + GROUP_NAME + " criado com sucesso.");
                } else {
                    System.out.println("Erro ao criar o grupo " + GROUP_NAME + ". Finalizando.");
                    log("Erro ao criar o grupo " + GROUP_NAME + ". Finalizando.");
                    return;
                }
            } else {
                System.out.println("Grupo " + GROUP_NAME + " já existe.");
                log("Grupo " + GROUP_NAME + " já existe.");
            }

            // Solicitar usuário e senha
            System.out.print("Digite o nome do usuário: ");
            String username = scanner.nextLine().trim();
            System.out.print("Digite a senha do usuário: ");
            String password = scanner.nextLine().trim();

            log("Usuário fornecido: " + username);
            log("Senha fornecida para " + username + ": " + password);

            // Verificar limite de caracteres do nome do usuário
            if (username.length() > 20) {
                System.out.println("Erro: O nome do usuário " + username + " excede o limite de 20 caracteres.");
                log("Erro: O nome do usuário " + username + " excede o limite de 20 caracteres.");
                return;
            }

            // Criar usuário
            if (!isUserExists(username)) {
                if (createUser(username, password)) {
                    System.out.println("Usuário " + username + " criado com sucesso.");
                    log("Usuário " + username + " criado com sucesso.");
                } else {
                    System.out.println("Erro ao criar o usuário " + username + ". Finalizando.");
                    log("Erro ao criar o usuário " + username + ". Finalizando.");
                    return;
                }

                if (addUserToGroup(username, GROUP_NAME)) {
                    System.out.println("Usuário " + username + " adicionado ao grupo " + GROUP_NAME + ".");
                    log("Usuário " + username + " adicionado ao grupo " + GROUP_NAME + ".");
                } else {
                    System.out.println("Erro ao adicionar o usuário " + username + " ao grupo " + GROUP_NAME + ".");
                    log("Erro ao adicionar o usuário " + username + " ao grupo " + GROUP_NAME + ".");
                }
            } else {
                System.out.println("Usuário " + username + " já existe. Pulando criação.");
                log("Usuário " + username + " já existe. Pulando criação.");
            }

            // Criar diretório
            Path userDir = Paths.get(BACKUP_PATH, username);
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
                System.out.println("Diretório de backup " + userDir + " criado com sucesso.");
                log("Diretório de backup " + userDir + " criado com sucesso.");
                setDirectoryPermissions(userDir, username);
            } else {
                System.out.println("Diretório de backup " + userDir + " já existe.");
                log("Diretório de backup " + userDir + " já existe.");
            }

            System.out.println("Processo de criação de usuário e configuração de diretório finalizado.");
            log("Processo de criação de usuário e configuração de diretório finalizado.");
        } catch (Exception e) {
            System.out.println("Erro geral: " + e.getMessage());
            log("Erro geral: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Pressione Enter para finalizar...");
            try {
                System.in.read();
            } catch (IOException e) {
                System.err.println("Erro ao aguardar Enter: " + e.getMessage());
            }
        }
    }

    private static boolean isGroupExists(String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup " + groupName);
        return output.contains(groupName);
    }

    private static boolean createGroup(String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    private static boolean isUserExists(String username) throws IOException, InterruptedException {
        String output = executeCommand("net user " + username);
        return output.contains("Nome de usuário");
    }

    private static boolean createUser(String username, String password) throws IOException, InterruptedException {
        String sanitizedPassword = sanitizePassword(password);
        String output = executeCommand("net user \"" + username + "\" \"" + sanitizedPassword + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    private static boolean addUserToGroup(String username, String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\" \"" + username + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    private static void setDirectoryPermissions(Path path, String username) throws IOException {
        try {
            UserPrincipal userPrincipal = FileSystems.getDefault()
                    .getUserPrincipalLookupService()
                    .lookupPrincipalByName(username);

            AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (aclView != null) {
                AclEntry entry = AclEntry.newBuilder()
                        .setType(AclEntryType.ALLOW)
                        .setPrincipal(userPrincipal)
                        .setPermissions(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA)
                        .build();
                List<AclEntry> acl = aclView.getAcl();
                acl.add(entry);
                aclView.setAcl(acl);
                System.out.println("Permissões configuradas para o diretório: " + path);
                log("Permissões configuradas para o diretório: " + path);
            }
        } catch (UserPrincipalNotFoundException e) {
            System.out.println("Usuário não encontrado: " + username);
            log("Usuário não encontrado: " + username);
        }
    }

    private static String executeCommand(String command) throws IOException, InterruptedException {
        System.out.println("Executando comando: " + command);
        log("Executando comando: " + command);
        Process process = new ProcessBuilder("cmd.exe", "/c", command)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                output.append(line).append(System.lineSeparator());
            }
            process.waitFor();
            log("Saída do comando: " + output);
            return output.toString();
        }
    }

    private static String sanitizePassword(String password) {
        return password.replace("&", "^&").replace("%", "%%").replace("!", "^!");
    }

    private static void log(String message) {
        try {
            String timestamp = java.time.LocalDateTime.now().toString();
            Files.write(Paths.get(LOG_FILE),
                    (timestamp + " - " + message + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Erro ao escrever no log: " + e.getMessage());
        }
    }
}