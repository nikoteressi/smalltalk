package com.smalltalk.server;

import com.smalltalk.server.autorisation.AuthService;
import com.smalltalk.server.autorisation.InMemoryAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 8089;
    private AuthService authService;
    private List<ClientHandler> handlers;

    public Server() {
        this.authService = new InMemoryAuthService();
        this.handlers = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server start!");
            while (true) {
                System.out.println("Waiting for connection......");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                new ClientHandler(socket, this).handle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String from, String message) {
        message = String.format("[%s]: %s", from, message);
        for (ClientHandler handler : handlers) {
            handler.sendMessage(message);
        }
    }

    public synchronized void removeAuthorizedClientFromList(ClientHandler handler) {
        this.handlers.remove(handler);
        sendClientsOnline();
    }

    public synchronized void addAuthorizedClientToList(ClientHandler handler) {
        this.handlers.add(handler);
        sendClientsOnline();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void sendClientsOnline() {
        StringBuilder sb = new StringBuilder("$.list: ");
        for (ClientHandler handler : handlers) {
            sb.append(handler.getCurrentUser()).append(" ");
        }
        String message = sb.toString();
        for (ClientHandler handler : handlers) {
            handler.sendMessage(message);
        }
    }

    public void sendPrivateMessage(String sender, String recepient, String message, ClientHandler senderHandler) {
        for (ClientHandler handler : handlers) {
            if (handler.getCurrentUser().equals(recepient)) {
                message = String.format("[%s] -> [%s] : %s", sender, recepient, message);
                handler.sendMessage(message);
                return;
            }
        }
        senderHandler.sendMessage("ERROR: recipient not found");
    }
}
