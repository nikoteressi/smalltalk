package com.smalltalk.server.application;

import com.smalltalk.server.auth.AuthorizationService;
import com.smalltalk.server.auth.DatabaseAuthorizationService;
import com.smalltalk.server.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {
    private static final int PORT = 8089;
    private final AuthorizationService authorizationService;
    private Map<String, ClientHandler> usersList;
//    private Map<String, ClientHandler> handlersList;

    public ChatServer() {
        this.authorizationService = new DatabaseAuthorizationService();
        this.usersList = new HashMap<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server start!");
            authorizationService.start();
            while (true) {
                System.out.println("Waiting for connection......");
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");
                new ClientHandler(socket, this).handle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            authorizationService.stop();
        }
    }

    public void broadcastMessage(String sender, String message) {
        message = String.format("[%s]: %s", sender, message);
        for (ClientHandler handler : usersList.values()) {
            handler.sendMessage(message);
        }
    }

    public synchronized void removeAuthorizedClientFromList(ClientHandler userHandler) {
        this.usersList.remove(userHandler.getCurrentUser());
        sendClientsOnline();
    }

    public synchronized void addAuthorizedClientToList(ClientHandler userHandler) {
        this.usersList.put(userHandler.getCurrentUser(), userHandler);
        sendClientsOnline();
    }

    public AuthorizationService getAuthService() {
        return authorizationService;
    }

    public void sendClientsOnline() {
        StringBuilder sb = new StringBuilder("/list:").append(ClientHandler.REGEX);
        for (ClientHandler userHandler : usersList.values()) {
            sb.append(userHandler.getCurrentUser()).append(ClientHandler.REGEX);
        }
        String message = sb.toString();
        for (ClientHandler userHandler : usersList.values()) {
            userHandler.sendMessage(message);
        }
    }

    public void sendPrivateMessage(String sender, String recipient, String message, ClientHandler senderHandler) {
        ClientHandler recipientHandler = usersList.get(recipient);
        if (recipientHandler == null) {
            senderHandler.sendMessage(String.format("ERROR:%s recipient not found: %s", ClientHandler.REGEX, recipient));
            return;
        }
        message = String.format("[%s] -> [%s]: %s", sender, recipient, message);
        recipientHandler.sendMessage(message);
        senderHandler.sendMessage(message);
    }

    public boolean isNicknameBusy(String nickname) {
        return this.usersList.containsKey(nickname);
    }
}
