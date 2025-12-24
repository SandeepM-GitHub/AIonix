import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class AiConsole extends JFrame {

// Fields or Variables
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton voiceButton; // Adding a new button to call from python
    private List<String> commandHistory = new ArrayList<>(); // Variable for implementing
    private final List<Process> childProcesses = new ArrayList<>(); // Variable for implementing Process Life cycle Management
    private final Map<String, String> appAliasMap = new HashMap<>(); // apps Aliasing

// Variable for "Intent Router"
    private enum IntentType {
        SYSTEM,
        KNOWLEDGE,
        HYBRID,
        CONTROL
    }

// Constructors
    public AiConsole() {
        setTitle("AIonix OS Console");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JLabel header = new JLabel("AIonix - Intelligent OS Console");
        header.setForeground(new Color(200, 200, 200));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.setFont(new Font("Sanserif", Font.BOLD, 14));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 30, 34));
        headerPanel.add(header, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField = new JTextField();
        // Improving readability
        Font consoleFont = new Font("JetBrains Mono", Font.PLAIN, 20);
        chatArea.setFont(consoleFont);
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        inputField.setFont(consoleFont);
        inputField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        sendButton = new JButton("Send");
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);

        voiceButton = new JButton("🎤"); // This uses Python code file
        voiceButton.setFocusPainted(false);
        voiceButton.setBorderPainted(false);

        initAppAliases();

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
        }});

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
                speak(response);
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
                    speak(response);
                    inputField.setText("");
                }
            }
        };
        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanupChildProcesses();
        }));
    }

    // UI helper methods
    private void applyDarkTheme() {
        Color bg = new Color(24, 24, 27);
        Color fg = new Color(228, 228, 231);

        chatArea.setBackground(bg);
        chatArea.setForeground(fg);
        chatArea.setCaretColor(fg);

        inputField.setBackground(new Color(45, 45, 45));
        inputField.setForeground(fg);
        inputField.setCaretColor(new Color(0, 200, 150));

        sendButton.setBackground(new Color(60, 60, 65));
        sendButton.setForeground(fg);

        voiceButton.setBackground(new Color(60, 60, 65));
        voiceButton.setForeground(fg);
    }
    private void appendChat(String text) {
        chatArea.append(text + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // Implementation of App aliasing
    private void initAppAliases() {
        appAliasMap.put("notepad", "notepad");
        appAliasMap.put("calculator", "calc");
        appAliasMap.put("calc", "calc");
        appAliasMap.put("cmd", "wt");
        appAliasMap.put("terminal", "wt");
        appAliasMap.put("powershell", "wt");
    }
    // Implementation of "INTENT ROUTER"
    private IntentType routeIntent(String input) {
        String text = input.toLowerCase();

        // Control Intent
        if (text.equals("cmdhistory") || text.equals("cmdlast")
            || text.contains("speak") || text.contains("mute")
                || text.contains("stop")) {
            return IntentType.CONTROL;
        }

        // System Intent
        if (text.contains("open") || text.contains("list") 
                || text.contains("folder") || text.contains("path")) {
            // if asked for explanation then HYBRID
            if (text.contains("why") || text.contains("what") 
                    || text.contains("explain") || text.contains("how")) {
                return IntentType.HYBRID;
            }
            return IntentType.SYSTEM;
        }

        // Knowledge Intent
        if (text.contains("why") || text.contains("what") 
                || text.contains("explain") || text.contains("how")) {
            return IntentType.KNOWLEDGE;
        }

        // Default Knowledge Intent
        return IntentType.KNOWLEDGE;
    }

    // Command "Brain"
    private String handleCommand(String input) {
        // Handling Intents
        IntentType intent = routeIntent(input);

        switch (intent) {
            case SYSTEM:
                return handleSystemIntent(input);

            case KNOWLEDGE:
                return callAiModel(input);
            
            case HYBRID:
                handleSystemIntent(input);
                return callAiModel(input);
            
            case CONTROL:
                return handleSystemIntent(input);
        
            default:
                return callAiModel(input);
        }
    }

//System actions (OS-level)
    private String handleSystemIntent(String input) {
        String cmd = input.toLowerCase();

        if (cmd.startsWith("open ")) {
            String appName = cmd.substring(5).trim();
            String resolvedApp = 
            appAliasMap.getOrDefault(appName, appName);
            return openApplication(resolvedApp)
                ? "Opening " + appName
                : "Could not open " + appName;
                
        }
        else if (cmd.startsWith("list files in ")) {
            return listFilesIn(cmd.substring(14));
        }
        // Handles history of the commands given
        else if (cmd.equals("cmdhistory")) {
            if (commandHistory.isEmpty()) {
                return "No commands yet.";
            }
            StringBuilder sb = new StringBuilder("Command history:\n");
            for (int i = 0; i < commandHistory.size(); i++) {
                sb.append(i + 1).append(". ").append(commandHistory.get(i)).append("\n");
            }
            return sb.toString();
        }
        else if (cmd.equals("cmdlast")) {
            if (commandHistory.size() < 2) {
                return "No previous command.";
            }
            return "Last command was: " +
                    commandHistory.get(commandHistory.size() - 2);
        }
        // else if (cmd.equals("time")) {
        //     return java.time.LocalTime.now().toString();
        // }
        return "System command not recognized";
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

// Voice methods
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
    private void speak(String text) {
        try {
            Process process = new ProcessBuilder(
                "python",
                "voice_output.py",
                text
            ).start();

            childProcesses.add(process);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

// AI methods
    private String callAiModel(String userPrompt) {
        try {
            String apiKey = System.getenv("GROQ_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "Groq API key not found. Please set GROQ_API_KEY.";
            }

            String requestBody = """
                    {
                        "model" : "llama-3.1-8b-instant",
                        "messages": [
                        {"role": "system", "content": "You are an AI operating system assistant."},
                        {"role": "user", "content": "%s"}
                        ]
                    }
                    """.formatted(userPrompt);

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " +apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                    
                    HttpClient client = HttpClient.newHttpClient();
                    HttpResponse<String> response = 
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                        // System.out.println("RAW GROQ RESPONSE:\n" + response.body());

                    return extractAiText(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "Error contacting Groq AI service.";
        }

    }
    private String extractAiText(String json) {
        try {
            // Looking for assistant message content
            int contentIndex = json.indexOf("\"content\":");
            if (contentIndex == -1) {
                return "No AI response";
            }
            // Move past "content":
            int startQuote = json.indexOf("\"", contentIndex + 10);
            int endQuote = json.indexOf("\"", startQuote + 1);

            if (startQuote == -1 || endQuote == -1) {
                return "No AI response.";
            }
            return json.substring(startQuote + 1, endQuote)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
        } catch (Exception e) {
            return "Failed to parse AI response.";
        }
    }

// Clean-Up logic for Process Life cycle Management
    private void cleanupChildProcesses() {
        for (Process p : childProcesses) {
            try {
                if(p.isAlive()) {
                    p.destroy();
                }
            } catch (Exception ignored) {}
        }
    }
    
    
// Main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AiConsole console = new AiConsole();
            console.setVisible(true);
        });
    }
}
