import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class AiConsole extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private List<String> commandHistory = new ArrayList<>();
    private JButton voiceButton; // Adding a new button to call from python

    public AiConsole() {
        setTitle("AIonix OS Console");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        voiceButton = new JButton("🎤"); // This uses Python code file

        inputField.addKeyListener(new KeyAdapter() {
        int index = -1;

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (!commandHistory.isEmpty()) {
                    if (index < 0) index = commandHistory.size() - 1;
                    inputField.setText(commandHistory.get(index));
                    index = Math.max(0, index - 1);
                }
            }
        }
    });

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(1,2));
        buttons.add(sendButton);
        buttons.add(voiceButton);

        inputPanel.add(buttons, BorderLayout.EAST);
        // Voice Action listener block
        voiceButton.addActionListener(e -> {
            appendChat("🎤 Listening...");
            String voiceText = listenFromMic();

            if (!voiceText.isEmpty()) {
                appendChat("> " + voiceText);
                commandHistory.add(voiceText);
                String response = handleCommand(voiceText);
                appendChat("AI :: " + response);
            } else {
                appendChat("AI :: I didn't catch that.");
            }
    });

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        applyDarkTheme();

        // When you click Send or press Enter
        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userText = inputField.getText().trim();
                if (!userText.isEmpty()) {
                    appendChat("> " + userText);
                    commandHistory.add(userText);
                    String response = handleCommand(userText);
                    appendChat("AIonix : " + response);
                    inputField.setText("");
                }
            }
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
    }

    private void applyDarkTheme() {
    Color bg = new Color(30, 30, 30);
    Color fg = new Color(220, 220, 220);

    chatArea.setBackground(bg);
    chatArea.setForeground(fg);
    chatArea.setCaretColor(fg);

    inputField.setBackground(new Color(45, 45, 45));
    inputField.setForeground(fg);
    inputField.setCaretColor(fg);

    sendButton.setBackground(new Color(60, 60, 60));
    sendButton.setForeground(fg);
}

    private void appendChat(String text) {
    chatArea.append(text + "\n");
    chatArea.setCaretPosition(chatArea.getDocument().getLength());
}


    // For now this is the "brain"
    private String handleCommand(String input) {
    String cmd = input.toLowerCase();

    if (cmd.equals("history")) {
        if (commandHistory.isEmpty()) {
            return "No commands yet.";
        }
        StringBuilder sb = new StringBuilder("Command history:\n");
        for (int i = 0; i < commandHistory.size(); i++) {
            sb.append(i + 1).append(". ").append(commandHistory.get(i)).append("\n");
        }
        return sb.toString();
    }
    else if (cmd.equals("last command")) {
        if (commandHistory.size() < 2) {
            return "No previous command.";
        }
        return "Last command was: " +
                commandHistory.get(commandHistory.size() - 2);
    }
    else if (cmd.equals("time")) {
        return java.time.LocalTime.now().toString();
    }
    else if (cmd.startsWith("open ")) {
        String appName = input.substring(5);
        boolean ok = openApplication(appName);
        return ok ? "Opening " + appName : "I couldn't open " + appName;
    }
    else if (cmd.startsWith("list files in ")) {
        String path = input.substring("list files in ".length());
        return listFilesIn(path);
    }
    else {
        return "I don't understand that yet.";
    }
}


    private boolean openApplication(String appName) {
    try {
        ProcessBuilder builder;

        // Windows-specific handling
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            builder = new ProcessBuilder("cmd", "/c", appName);
        } else {
            // Linux / macOS
            builder = new ProcessBuilder(appName);
        }

        builder.start();
        return true;

    } catch (Exception e) {
        e.printStackTrace();
        return false;
    } 
}
    private String listenFromMic() {
    try {
        ProcessBuilder pb = new ProcessBuilder("python", "voice_input.py");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        process.waitFor();

        return sb.toString().toLowerCase().trim();

    } catch (Exception e) {
        e.printStackTrace();
        return "";
    }
}



    private String listFilesIn(String path) {
        java.io.File folder = new java.io.File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            return "Path doesn't exist or is not a directory: " + path;
        }
        StringBuilder sb = new StringBuilder("Files in " + path + ":\n");
        java.io.File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            sb.append("(no files)");
        } else {
            for (java.io.File f : files) {
                sb.append(" - ").append(f.getName()).append("\n");
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AiConsole console = new AiConsole();
            console.setVisible(true);
        });
    }
}
