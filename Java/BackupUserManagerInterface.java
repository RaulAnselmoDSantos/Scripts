import javax.swing.*;
// import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.Scanner;

public class BackupUserManagerInterface extends JFrame {

    private JCheckBox useDefaultFileCheckBox;
    private JTextArea logArea;
    private JButton startButton;

    private static final String DEFAULT_FILE_PATH = "C:\\Scripts\\utilizados\\usuarios_bkp2.txt";

    public BackupUserManagerInterface() {
        setTitle("Backup User Manager");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel companyNameLabel = new JLabel("Informatica F1", SwingConstants.CENTER);
        companyNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(companyNameLabel, BorderLayout.CENTER);

        JPanel versionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        versionPanel.add(new JLabel("Version: 1.0.0"));
        versionPanel.add(new JLabel("Created by: Raul"));
        headerPanel.add(versionPanel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Use default file checkbox
        useDefaultFileCheckBox = new JCheckBox("Usar Arquivo Padrão?");
        useDefaultFileCheckBox.addActionListener(e -> toggleFileSelection());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(useDefaultFileCheckBox, gbc);

        // Log area
        JLabel logLabel = new JLabel("Logs:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        mainPanel.add(logLabel, gbc);

        logArea = new JTextArea(15, 40);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        gbc.gridy = 2;
        mainPanel.add(logScrollPane, gbc);

        // Start button
        startButton = new JButton("Iniciar");
        startButton.addActionListener(e -> startProcess());
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(startButton, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void toggleFileSelection() {
        if (useDefaultFileCheckBox.isSelected()) {
            logArea.append("Usando o arquivo padrão: " + DEFAULT_FILE_PATH + "\n");
        }
    }

    private void startProcess() {
        logArea.append("Iniciando o processo...\n");

        if (useDefaultFileCheckBox.isSelected()) {
            executeBackupScript();
        } else {
            logArea.append("Erro: Nenhum arquivo selecionado.\n");
        }

        logArea.append("Processo concluído.\n");
    }

    private void executeBackupScript() {
        try {
            // Caminho atualizado para o diretório atual
            String currentDirectory = new File(".").getAbsolutePath();
            Process process = new ProcessBuilder("java", "-cp", currentDirectory, "BackupUserManager")
                    .redirectErrorStream(true)
                    .start();
    
            // Captura da saída do processo
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    logArea.append(scanner.nextLine() + "\n");
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logArea.append("Erro ao executar o script de backup: " + e.getMessage() + "\n");
        }
    }
    


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BackupUserManagerInterface frame = new BackupUserManagerInterface();
            frame.setVisible(true);
        });
    }
}