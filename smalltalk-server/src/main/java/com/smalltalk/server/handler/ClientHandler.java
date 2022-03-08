package com.smalltalk.server.handler;

import com.smalltalk.server.application.ChatServer;
import com.smalltalk.server.error.UserNotFoundException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Handler{
    public static final String REGEX = "%&%";
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread handlerThread;
    private ChatServer server;
    private String currentUser;

    public ClientHandler(Socket socket, ChatServer server) {
        try {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Handler created");
            this.server = server;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handle() {
        handlerThread = new Thread(() -> {
            authorize();
            try {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    String message = in.readUTF();
                    handleMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                server.removeAuthorizedClientFromList(this);
            }
        });
        handlerThread.start();
    }

    private void authorize() {
        while (!socket.isClosed()) {
            try {
                String message = in.readUTF();
                if (message.startsWith("/auth") || message.startsWith("/register")) {
                    if (handleMessage(message)) break;
                }
            } catch (SocketException e) {
                System.out.println("Socket closed with timeout");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean handleMessage(String message) {
        try {
            String[] parsed = message.split(REGEX);
            switch (parsed[0]) {
                case "/w":
                    server.sendPrivateMessage(this.currentUser, parsed[1], parsed[2], this);
                    break;
                case "/ALL":
                    server.broadcastMessage(this.currentUser, parsed[1]);
                    break;
                case "/change_nick":
                    String nick = server.getAuthService().changeNickname(this.currentUser, parsed[1]);
                    server.removeAuthorizedClientFromList(this);
                    this.currentUser = nick;
                    server.addAuthorizedClientToList(this);
                    sendMessage("/change_nick_ok");
                    break;
                case "/change_pass":
                    server.getAuthService().changePassword(this.currentUser, parsed[1], parsed[2]);
                    sendMessage("/change_pass_ok");
                    break;
                case "/remove":
                    server.getAuthService().deleteUser(this.currentUser);
                    this.socket.close();
                    break;
                case "/register":
                    server.getAuthService().createNewUser(parsed[1], parsed[2], parsed[3]);
                    sendMessage("register_ok:");
                    break;
                case "/authorization":
                    try {
                        String username = server.getAuthService().getNicknameByLoginAndPassword(parsed[1], parsed[2]);
                        if (server.isNicknameBusy(username)) {
                            sendMessage("ERROR:" + REGEX + "You already signed in!");
                        } else {
                            this.currentUser = username;
                            this.server.addAuthorizedClientToList(this);
                            sendMessage("authorization_ok:" + REGEX + this.currentUser);
                            return true;
                        }
                    } catch (UserNotFoundException e) {
                        System.out.println("Auth error");
                        sendMessage("ERROR:" + REGEX + "Wrong username or pass");
                    }
                    break;
                default:
                    sendMessage("ERROR:" + REGEX + "command not found!");
            }
        } catch (Exception e) {
            sendMessage("ERROR:" + REGEX + e.getMessage());
        }
        return false;
    }

    public Thread getHandlerThread() {
        return handlerThread;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void sendMessage(String message) {
        try {
            this.out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}