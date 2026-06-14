package chat.server;

import chat.common.Message;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor de Chat
 * - TCP: aceita conexões de clientes e faz broadcast das mensagens
 * - UDP: responde pings para medição de latência
 * - Multithreading: uma thread por cliente TCP + thread dedicada ao UDP
 */
public class ChatServer {

    public static final int TCP_PORT = 12345;
    public static final int UDP_PORT = 12346;

    // Lista thread-safe de todos os clientes conectados
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public void start() {
        System.out.println("=== Servidor de Chat iniciado ===");
        System.out.println("TCP na porta: " + TCP_PORT);
        System.out.println("UDP na porta: " + UDP_PORT);

        // Thread para responder pings UDP
        Thread udpThread = new Thread(this::runUdpPingResponder, "UDP-Ping-Responder");
        udpThread.setDaemon(true);
        udpThread.start();

        // Thread principal: aceita conexões TCP
        runTcpAcceptor();
    }

    // -------------------------------------------------------------------------
    // TCP: aceita novos clientes em loop
    // -------------------------------------------------------------------------
    private void runTcpAcceptor() {
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("Aguardando conexões TCP...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);

                Thread t = new Thread(handler, "Client-" + clientSocket.getInetAddress());
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor TCP: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // UDP: responde a qualquer pacote recebido (ping-pong para medir latência)
    // -------------------------------------------------------------------------
    private void runUdpPingResponder() {
        try (DatagramSocket udpSocket = new DatagramSocket(UDP_PORT)) {
            byte[] buffer = new byte[64];
            System.out.println("Aguardando pings UDP...");
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                // Devolve exatamente o mesmo pacote (pong)
                DatagramPacket response = new DatagramPacket(
                        packet.getData(), packet.getLength(),
                        packet.getAddress(), packet.getPort());
                udpSocket.send(response);
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor UDP: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Broadcast: envia mensagem para todos os clientes conectados
    // -------------------------------------------------------------------------
    public void broadcast(Message message, ClientHandler origin) {
        System.out.println("Broadcast: " + message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Remove cliente da lista quando ele desconectar
    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("Cliente removido. Total conectado: " + clients.size());
    }

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        new ChatServer().start();
    }
}
