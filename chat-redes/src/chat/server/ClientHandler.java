package chat.server;

import chat.common.Message;

import java.io.*;
import java.net.Socket;

/**
 * Roda em sua própria thread.
 * Lê mensagens do cliente via TCP e faz broadcast para todos.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private ObjectOutputStream out;
    private String clientName = "Anônimo";

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            // Primeira mensagem é o nome do usuário
            Message first = (Message) in.readObject();
            clientName = first.getSender();
            System.out.println("Usuário identificado: " + clientName);

            // Anuncia entrada no chat
            server.broadcast(new Message("Servidor", clientName + " entrou no chat."), this);

            // Loop: lê e faz broadcast das mensagens
            Message msg;
            while ((msg = (Message) in.readObject()) != null) {
                server.broadcast(msg, this);
            }

        } catch (EOFException | java.net.SocketException e) {
            // Cliente desconectou normalmente
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro com cliente " + clientName + ": " + e.getMessage());
        } finally {
            server.removeClient(this);
            server.broadcast(new Message("Servidor", clientName + " saiu do chat."), this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void sendMessage(Message message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
                out.reset(); // evita cache de objetos
            }
        } catch (IOException e) {
            System.err.println("Erro ao enviar para " + clientName + ": " + e.getMessage());
        }
    }
}
