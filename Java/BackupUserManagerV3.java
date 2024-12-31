import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.Consumer;

public class BackupUserManagerV3 {

    public static final String BACKUP_PATH = "";
    public static final String LOG_FILE = "";
    public static final String GROUP_NAME = "";

    private static Consumer<String> logConsumer = System.out::println;

    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer != null ? consumer : System.out::println;
    }

    public static void log(String message) {
        logConsumer.accept(message);
        try {
            String timestamp = java.time.LocalDateTime.now().toString();
            Files.write(Paths.get(LOG_FILE),
                    (timestamp + " - " + message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Erro ao escrever no log: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            log("Erro: Caminho para o arquivo de usuários não fornecido.");
            return;
        }

        String usersFile = args[0];
        log("Iniciando o script para criação de usuários em lote.");

        try {
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

            log("Lendo arquivo de usuários em " + usersFile + ".");
            List<User> users = readUsersFromFile(usersFile);
            if (users.isEmpty()) {
                log("Nenhum usuário válido encontrado. Finalizando.");
                return;
            }

            for (User user : users) {
                log("Processando usuário: " + user.getUsername());
                if (!isUserExists(user.getUsername())) {
                    if (createUser(user)) {
                        log("Usuário " + user.getUsername() + " criado com sucesso.");
                        if (addUserToGroup(user.getUsername(), GROUP_NAME)) {
                            log("Usuário " + user.getUsername() + " adicionado ao grupo " + GROUP_NAME + ".");
                        } else {
                            log("Erro ao adicionar o usuário " + user.getUsername() + " ao grupo.");
                        }
                    } else {
                        log("Erro ao criar o usuário " + user.getUsername() + ".");
                    }
                } else {
                    log("Usuário " + user.getUsername() + " já existe.");
                }

                Path userDir = Paths.get(BACKUP_PATH, user.getUsername());
                log("Tentando criar diretório em: " + userDir);
                if (!Files.exists(userDir)) {
                    Files.createDirectories(userDir);
                    log("Diretório de backup " + userDir + " criado com sucesso.");
                    setDirectoryPermissions(userDir, user.getUsername());
                } else {
                    log("Diretório de backup " + userDir + " já existe.");
                }
            }

            log("Processo de criação de usuários em lote finalizado.");
        } catch (Exception e) {
            log("Erro geral: " + e.getMessage());
        }
    }

    public static boolean isGroupExists(String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup " + groupName);
        return output.contains(groupName);
    }

    public static boolean createGroup(String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    public static boolean isUserExists(String username) throws IOException, InterruptedException {
        String output = executeCommand("net user " + username);
        return output.contains("Nome de usuário");
    }

    public static boolean createUser(User user) throws IOException, InterruptedException {
        String username = user.getUsername();
        String password = user.getPassword();
    
        // Logar os dados antes de criar o usuário
        log("Tentando criar usuário com:");
        log("Usuário: " + username);
        log("Senha: " + password);
    
        // Substituir os caracteres especiais no comando
        String escapedPassword = password.replace("&", "&").replace("%", "%").replace("!", "!");
        String command = "net user \"" + username + "\" \"" + escapedPassword + "\" /add";
    
        // Executar comando
        log("Executando comando: " + command);
        String output = executeCommand(command);
        log("Saída do comando: " + output);
    
        return !output.toLowerCase().contains("erro");
    }
    

    public static boolean addUserToGroup(String username, String groupName) throws IOException, InterruptedException {
        String output = executeCommand("net localgroup \"" + groupName + "\" \"" + username + "\" /add");
        return !output.toLowerCase().contains("erro");
    }

    public static List<User> readUsersFromFile(String filePath) throws IOException {
        List<User> users = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    users.add(new User(parts[0].trim(), parts[1].trim()));
                } else {
                    log("Linha inválida no arquivo: " + line);
                }
            }
        }
        return users;
    }

    public static void setDirectoryPermissions(Path path, String username) throws IOException {
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

    public static String executeCommand(String command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("cmd.exe", "/c", command)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            process.waitFor();
            return output.toString();
        }
    }

    public static class User {
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
