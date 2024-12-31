import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class BackupUserManagerV2 {

    private static final String BACKUP_PATH = "D:\\BACKUPS\\";
    private static final String LOG_FILE = "C:\\Scripts\\logs\\logfile.txt";
    private static final String USERS_FILE = "C:\\Scripts\\utilizados\\usuarios_bkp_JAVA.txt";
    private static final String GROUP_NAME = "F1SFTP2";

    public static void main(String[] args) {
        System.out.println("Iniciando o script para criação de usuários e diretórios.");
        log("Iniciando o script para criação de usuários e diretórios.");

        try {
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

            // Ler usuários do arquivo
            System.out.println("Lendo arquivo de usuários em " + USERS_FILE + ".");
            log("Lendo arquivo de usuários em " + USERS_FILE + ".");
            List<User> users = readUsersFromFile(USERS_FILE);
            if (users.isEmpty()) {
                System.out.println("Nenhum usuário válido encontrado. Finalizando.");
                log("Nenhum usuário válido encontrado. Finalizando.");
                return;
            }

            // Criar usuários e configurar diretórios
            for (User user : users) {
                System.out.println("Processando usuário: " + user.getUsername());
                log("Processando usuário: " + user.getUsername());
                log("Senha usada para " + user.getUsername() + ": " + user.getPassword());

                // Verificar limite de caracteres do nome do usuário
                if (user.getUsername().length() > 20) {
                    System.out.println("Erro: O nome do usuário " + user.getUsername() + " excede o limite de 20 caracteres.");
                    log("Erro: O nome do usuário " + user.getUsername() + " excede o limite de 20 caracteres.");
                    continue;
                }

                // Criar usuário
                if (!isUserExists(user.getUsername())) {
                    if (createUser(user)) {
                        System.out.println("Usuário " + user.getUsername() + " criado com sucesso.");
                        log("Usuário " + user.getUsername() + " criado com sucesso.");
                    } else {
                        System.out.println("Erro ao criar o usuário " + user.getUsername() + ". Pulando.");
                        log("Erro ao criar o usuário " + user.getUsername() + ". Pulando.");
                        continue;
                    }

                    if (addUserToGroup(user.getUsername(), GROUP_NAME)) {
                        System.out.println("Usuário " + user.getUsername() + " adicionado ao grupo " + GROUP_NAME + ".");
                        log("Usuário " + user.getUsername() + " adicionado ao grupo " + GROUP_NAME + ".");
                    } else {
                        System.out.println("Erro ao adicionar o usuário " + user.getUsername() + " ao grupo " + GROUP_NAME + ".");
                        log("Erro ao adicionar o usuário " + user.getUsername() + " ao grupo " + GROUP_NAME + ".");
                    }
                } else {
                    System.out.println("Usuário " + user.getUsername() + " já existe. Pulando criação.");
                    log("Usuário " + user.getUsername() + " já existe. Pulando criação.");
                }

                // Criar diretório
                Path userDir = Paths.get(BACKUP_PATH, user.getUsername());
                if (!Files.exists(userDir)) {
                    Files.createDirectories(userDir);
                    System.out.println("Diretório de backup " + userDir + " criado com sucesso.");
                    log("Diretório de backup " + userDir + " criado com sucesso.");
                    setDirectoryPermissions(userDir, user.getUsername());
                } else {
                    System.out.println("Diretório de backup " + userDir + " já existe.");
                    log("Diretório de backup " + userDir + " já existe.");
                }
            }

            System.out.println("Processo de criação de usuários e configuração de diretórios finalizado.");
            log("Processo de criação de usuários e configuração de diretórios finalizado.");
        } catch (Exception e) {
            System.out.println("Erro geral: " + e.getMessage());
            log("Erro geral: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Pressione Enter para finalizar...");
            try {
                System.in.read(); // Aguarda o Enter para fechar
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

    static boolean createUser(User user) throws IOException, InterruptedException {
        String password = user.getPassword().replace("&", "&").replace("%", "%").replace("!", "!");
        String output = executeCommand("net user \"" + user.getUsername() + "\" \"" + password + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    private static boolean addUserToGroup(String username, String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\" \"" + username + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    private static List<User> readUsersFromFile(String filePath) throws IOException {
        List<User> users = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    users.add(new User(parts[0].trim(), parts[1].trim()));
                } else {
                    System.out.println("Linha inválida no arquivo: " + line);
                    log("Linha inválida no arquivo: " + line);
                }
            }
        }
        return users;
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

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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

    private static void log(String message) {
        try {
            String timestamp = java.time.LocalDateTime.now().toString();
            Files.write(Paths.get(LOG_FILE),
                    (timestamp + " - " + message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Erro ao escrever no log: " + e.getMessage());
        }
    }

    static class User {
        private final String username;
        private final String password;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}