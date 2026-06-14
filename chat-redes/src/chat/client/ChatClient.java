package chat.client;

import chat.common.Message;
import chat.server.ChatServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

/**
 * Cliente de Chat com interface Swing
 *
 * Threads:
 *  1. EDT (Event Dispatch Thread) - interface gráfica Swing
 *  2. TCP Receiver Thread          - recebe mensagens do servidor via TCP
 *  3. UDP Ping Thread              - envia pings a cada 2s e exibe latência
 */
public class ChatClient extends JFrame {

    // --- Componentes Swing ---
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel latencyLabel;
    private JLabel statusLabel;

    // --- Rede ---
    private String serverIp;
    private String username;
    private Socket tcpSocket;
    private ObjectOutputStream tcpOut;
    private DatagramSocket udpSocket;

    // --- Estado ---
    private volatile boolean running = false;

    // -------------------------------------------------------------------------
    // Construtor: monta a interface gráfica
    // -------------------------------------------------------------------------
    public ChatClient() {
        super("Chat em Rede - Redes de Computadores I");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Barra de status superior
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setBorder(new EmptyBorder(8, 8, 0, 8));
        statusLabel = new JLabel("Desconectado");
        statusLabel.setForeground(Color.RED);
        latencyLabel = new JLabel("Latência UDP: --");
        latencyLabel.setForeground(new Color(0, 100, 180));
        topPanel.add(statusLabel, BorderLayout.WEST);
        topPanel.add(latencyLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Área de chat (centro)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setMargin(new Insets(4, 6, 4, 6));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chat (TCP)"));
        add(scrollPane, BorderLayout.CENTER);

        // Painel inferior: campo de texto + botão
        JPanel bottomPanel = new JPanel(new BorderLayout(6, 0));
        bottomPanel.setBorder(new EmptyBorder(0, 8, 8, 8));
        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        inputField.setEnabled(false);
        sendButton = new JButton("Enviar");
        sendButton.setEnabled(false);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Ações de envio
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Ao fechar a janela, desconecta limpo
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });

        // Pede dados de conexão ao abrir
        SwingUtilities.invokeLater(this::showConnectionDialog);
    }

    // -------------------------------------------------------------------------
    // Diálogo de conexão (IP, porta, nome)
    // -------------------------------------------------------------------------
    private void showConnectionDialog() {
        JTextField ipField   = new JTextField("127.0.0.1", 15);
        JTextField nameField = new JTextField("Usuario", 15);

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("IP do Servidor:"));
        panel.add(ipField);
        panel.add(new JLabel("Seu nome:"));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Conectar ao Servidor",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            serverIp = ipField.getText().trim();
            username = nameField.getText().trim();
            if (username.isEmpty()) username = "Anônimo";
            connect();
        } else {
            System.exit(0);
        }
    }

    // -------------------------------------------------------------------------
    // Conexão TCP + UDP
    // -------------------------------------------------------------------------
    private void connect() {
        try {
            // Conexão TCP
            tcpSocket = new Socket(serverIp, ChatServer.TCP_PORT);
            tcpOut    = new ObjectOutputStream(tcpSocket.getOutputStream());
            tcpOut.flush();

            // UDP socket (não conectado; enviaremos para o servidor)
            udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(2000); // timeout para o pong

            running = true;

            // Envia identificação (primeiro pacote = nome do usuário)
            tcpOut.writeObject(new Message(username, ""));
            tcpOut.flush();

            // Atualiza UI
            statusLabel.setText("Conectado como: " + username + " → " + serverIp);
            statusLabel.setForeground(new Color(0, 140, 0));
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.requestFocus();

            // Inicia threads de recepção e ping
            startTcpReceiverThread();
            startUdpPingThread();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Não foi possível conectar:\n" + e.getMessage(),
                    "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            showConnectionDialog();
        }
    }

    // -------------------------------------------------------------------------
    // Thread 2: recebe mensagens TCP do servidor
    // -------------------------------------------------------------------------
    private void startTcpReceiverThread() {
        Thread t = new Thread(() -> {
            try (ObjectInputStream in = new ObjectInputStream(tcpSocket.getInputStream())) {
                while (running) {
                    Message msg = (Message) in.readObject();
                    appendToChat(msg.toString());
                }
            } catch (EOFException | SocketException e) {
                if (running) appendToChat("*** Conexão encerrada pelo servidor ***");
            } catch (IOException | ClassNotFoundException e) {
                if (running) appendToChat("*** Erro de recepção: " + e.getMessage() + " ***");
            }
        }, "TCP-Receiver");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------------------
    // Thread 3: envia pings UDP a cada 2 segundos e mede latência
    // -------------------------------------------------------------------------
    private void startUdpPingThread() {
        Thread t = new Thread(() -> {
            InetAddress serverAddr;
            try {
                serverAddr = InetAddress.getByName(serverIp);
            } catch (UnknownHostException e) {
                return;
            }

            byte[] pingData = "PING".getBytes();

            while (running) {
                try {
                    // Envia ping
                    DatagramPacket ping = new DatagramPacket(
                            pingData, pingData.length, serverAddr, ChatServer.UDP_PORT);
                    long sent = System.currentTimeMillis();
                    udpSocket.send(ping);

                    // Aguarda pong
                    byte[] buffer = new byte[64];
                    DatagramPacket pong = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(pong);
                    long rtt = System.currentTimeMillis() - sent;

                    // Atualiza label na EDT
                    SwingUtilities.invokeLater(() ->
                            latencyLabel.setText("Latência UDP: " + rtt + " ms"));

                    Thread.sleep(2000);

                } catch (SocketTimeoutException e) {
                    SwingUtilities.invokeLater(() ->
                            latencyLabel.setText("Latência UDP: timeout"));
                } catch (IOException | InterruptedException e) {
                    if (running) {
                        SwingUtilities.invokeLater(() ->
                                latencyLabel.setText("Latência UDP: erro"));
                    }
                    break;
                }
            }
        }, "UDP-Ping");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------------------------
    // Envia mensagem de chat via TCP
    // -------------------------------------------------------------------------
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || tcpOut == null) return;

        try {
            tcpOut.writeObject(new Message(username, text));
            tcpOut.flush();
            tcpOut.reset();
            inputField.setText("");
        } catch (IOException e) {
            appendToChat("*** Erro ao enviar mensagem: " + e.getMessage() + " ***");
        }
    }

    // -------------------------------------------------------------------------
    // Utilitário: adiciona texto na área de chat (sempre na EDT)
    // -------------------------------------------------------------------------
    private void appendToChat(String text) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(text + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // -------------------------------------------------------------------------
    // Desconecta limpo
    // -------------------------------------------------------------------------
    private void disconnect() {
        running = false;
        try { if (udpSocket != null) udpSocket.close(); } catch (Exception ignored) {}
        try { if (tcpSocket != null) tcpSocket.close(); } catch (Exception ignored) {}
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClient client = new ChatClient();
            client.setVisible(true);
        });
    }
}
