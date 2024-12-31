import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;

public class BackupUserManager {

    private static final String BACKUP_PATH = "";
    private static final String LOG_FILE = "";
    private static final String USERS_FILE = "";
    private static final String GROUP_NAME = "";

    public static void main(String[] args) {
        log("Iniciando o script para criação de usuários e diretórios.");

        try {
            // Verificar e criar grupo
            log("Verificando se o grupo " + GROUP_NAME + " existe.");
            if (!isGroupExists(GROUP_NAME)) {
                if (createGroup(GROUP_NAME)) {
                    log("Grupo " + GROUP_NAME + " criado com sucesso.");
                } else {
                    log("Erro ao criar o grupo " + GROUP_NAME + ". Finalizando.");
                    return;
                }
            } else {
                log("Grupo " + GROUP_NAME + " já existe.");
            }

            // Ler usuários do arquivo
            log("Lendo arquivo de usuários em " + USERS_FILE + ".");
            List<User> users = readUsersFromFile(USERS_FILE);
            if (users.isEmpty()) {
                log("Nenhum usuário válido encontrado. Finalizando.");
                return;
            }

            // Criar um pool de threads
            ExecutorService executor = Executors.newFixedThreadPool(5); // Define 5 threads no pool (ajustável)

            for (User user : users) {
                executor.submit(() -> processUser(user));
            }

            // Finalizar o pool de threads
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS); // Aguarda até 1 hora para a conclusão de todas as tarefas

            log("Processo de criação de usuários e configuração de diretórios finalizado.");
        } catch (Exception e) {
            log("Erro geral: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processUser(User user) {
        try {
            log("Processando usuário: " + user.getUsername());

            // Verificar limite de caracteres do nome do usuário
            if (user.getUsername().length() > 20) {
                log("Erro: O nome do usuário " + user.getUsername() + " excede o limite de 20 caracteres.");
                return;
            }

            // Criar usuário
            if (!isUserExists(user.getUsername())) {
                if (createUser(user)) {
                    log("Usuário " + user.getUsername() + " criado com sucesso.");
                } else {
                    log("Erro ao criar o usuário " + user.getUsername() + ". Pulando.");
                    return;
                }
            } else {
                log("Usuário " + user.getUsername() + " já existe.");
            }

            // Adicionar ao grupo
            if (!isUserInGroup(user.getUsername(), GROUP_NAME)) {
                if (addUserToGroup(user.getUsername(), GROUP_NAME)) {
                    log("Usuário " + user.getUsername() + " adicionado ao grupo " + GROUP_NAME + ".");
                } else {
                    log("Erro ao adicionar o usuário " + user.getUsername() + " ao grupo " + GROUP_NAME + ".");
                }
            } else {
                log("Usuário " + user.getUsername() + " já é membro do grupo " + GROUP_NAME + ".");
            }

            // Criar diretório
            Path userDir = Paths.get(BACKUP_PATH, user.getUsername());
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
                log("Diretório de backup " + userDir + " criado com sucesso.");
                setDirectoryPermissions(userDir, user.getUsername());
            } else {
                log("Diretório de backup " + userDir + " já existe.");
            }
        } catch (Exception e) {
            log("Erro ao processar usuário " + user.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isGroupExists(String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\"");
        return output.contains(groupName);
    }

    private static boolean isUserInGroup(String username, String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\"");
        return output.contains(username);
    }

    private static boolean createGroup(String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    private static boolean isUserExists(String username) throws IOException, InterruptedException {
        String output = executeCommand("net user \"" + username + "\"");
        return output.contains("Nome de usuário");
    }

    private static boolean createUser(User user) throws IOException, InterruptedException {
        String password = user.getPassword().replace("&", "^&").replace("%", "%%").replace("!", "^!");
        String output = executeCommand("net user \"" + user.getUsername() + "\" \"" + password + "\" /add");
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
                log("Permissões configuradas para o diretório: " + path);
            }
        } catch (UserPrincipalNotFoundException e) {
            log("Usuário não encontrado: " + username);
        }
    }

    private static List<User> readUsersFromFile(String filePath) throws IOException {
        List<User> users = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length == 2) {
                users.add(new User(parts[0].trim(), parts[1].trim()));
            } else {
                log("Linha inválida no arquivo: " + line);
            }
        }

        return users;
    }

    private static String executeCommand(String command) throws IOException, InterruptedException {
        System.out.println("Executando comando: " + command);
        Process process = new ProcessBuilder("cmd.exe", "/c", command)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            process.waitFor();
            System.out.println("Saída do comando: " + output);
            return output.toString();
        }
    }

    private static void log(String message) {
        try {
            String timestamp = java.time.LocalDateTime.now().toString();
            Files.write(Paths.get(LOG_FILE), (timestamp + " - " + message + System.lineSeparator()).getBytes(),
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
