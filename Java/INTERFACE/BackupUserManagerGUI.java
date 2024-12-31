import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class BackupUserManagerGUI extends JFrame {

    private static final String BATCH_USERS_FILE = "";

    private JTextArea logArea;
    private JTextField userField;
    private JPasswordField passField;

    public BackupUserManagerGUI() {
        setTitle("Gerenciador de Backup - F1");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header
        JLabel headerLabel = new JLabel("Gerenciador de Backup - F1", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(headerLabel, BorderLayout.NORTH);

        // Center panel
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Button to execute batch process
        JButton batchButton = new JButton("Criar Usuários em Lote");
        batchButton.addActionListener(this::executeBatchProcess);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        centerPanel.add(batchButton, gbc);

        // Divider
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        centerPanel.add(new JLabel("Ou"), gbc);

        // User field
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Usuário:"), gbc);

        userField = new JTextField(20);
        gbc.gridx = 1;
        centerPanel.add(userField, gbc);

        // Password field
        gbc.gridy = 3;
        gbc.gridx = 0;
        centerPanel.add(new JLabel("Senha:"), gbc);

        passField = new JPasswordField(20);
        gbc.gridx = 1;
        centerPanel.add(passField, gbc);

        // Button to create single user
        JButton singleUserButton = new JButton("Criar Usuário");
        singleUserButton.addActionListener(this::createUserProcess);
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerPanel.add(singleUserButton, gbc);

        add(centerPanel, BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.SOUTH);
    }

    private void executeBatchProcess(ActionEvent e) {
        log("Executando processo em lote...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Redirect log output
                BackupUserManagerV3.setLogConsumer(this::log);
                // Execute batch process
                BackupUserManagerV3.main(new String[] {BATCH_USERS_FILE});
            } catch (Exception ex) {
                log("Erro ao executar processo em lote: " + ex.getMessage());
            }
        });
    }

    private void createUserProcess(ActionEvent e) {
    String username = userField.getText().trim();
    String password = new String(passField.getPassword()).trim();

    if (username.isEmpty() || password.isEmpty()) {
        log("Usuário ou senha não podem estar vazios.");
        return;
    }

    log("Criando usuário: " + username);
    Executors.newSingleThreadExecutor().execute(() -> {
        try {
            if (!BackupUserManagerV3.isUserExists(username)) {
                // Tentativa de criação do usuário
                if (BackupUserManagerV3.createUser(new BackupUserManagerV3.User(username, password))) {
                    log("Usuário " + username + " criado com sucesso.");
                    
                    // Adicionando o usuário ao grupo
                    if (BackupUserManagerV3.addUserToGroup(username, BackupUserManagerV3.GROUP_NAME)) {
                        log("Usuário " + username + " adicionado ao grupo " + BackupUserManagerV3.GROUP_NAME + ".");
                    } else {
                        log("Erro ao adicionar o usuário " + username + " ao grupo.");
                    }

                    // Criar diretório para o usuário
                    log("Tentando criar diretório para o usuário: " + username);
                    Path userDir = Paths.get(BackupUserManagerV3.BACKUP_PATH, username);
                    if (!Files.exists(userDir)) {
                        Files.createDirectories(userDir);
                        log("Diretório criado: " + userDir);
                        // Configurar permissões
                        try {
                            BackupUserManagerV3.setDirectoryPermissions(userDir, username);
                            log("Permissões configuradas para o diretório: " + userDir);
                        } catch (IOException ex) {
                            log("Erro ao configurar permissões para o diretório: " + ex.getMessage());
                        }
                    } else {
                        log("Diretório já existe para o usuário: " + username);
                    }
                } else {
                    log("Erro ao criar o usuário " + username + ".");
                }
            } else {
                log("Usuário " + username + " já existe.");
            }
        } catch (Exception ex) {
            log("Erro ao criar usuário ou diretório: " + ex.getMessage());
        }
    });
}

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalDateTime.now().toString();
            logArea.append(timestamp + " - " + message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BackupUserManagerGUI gui = new BackupUserManagerGUI();
            gui.setVisible(true);
        });
    }
}
