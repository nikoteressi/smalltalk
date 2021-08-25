package com.smalltalk.server;

import com.smalltalk.server.error.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    public static final String SPLITER = "##@$";
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread handlerThread;
    private Server server;
    private String currentUser;

    public ClientHandler(Socket socket, Server server) {
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
                while (!Thread.currentThread().isInterrupted() && socket.isConnected()) {
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

    //auth: lllll ppppp
    private void authorize() {
        while (true) {
            try {
                String message = in.readUTF();
                if (message.startsWith("auth:")) {
                    String[] credentials = message.substring(6).split("\\s");
                    try {
                        this.currentUser = server.getAuthService().getNicknameByLoginAndPassword(credentials[0], credentials[1]);
                        this.server.addAuthorizedClientToList(this);
                        sendMessage("authok:" + SPLITER + this.currentUser);
                        break;
                    } catch (WrongCredentialsException e) {
                        sendMessage("ERROR: Wrong credentials");
                    } catch (UserNotFoundException e) {
                        sendMessage("ERROR: User not found!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(String message) {
        String[] parsed = message.split(SPLITER);
        switch (parsed[0]) {
            case "/w":
                server.sendPrivateMessage(this.currentUser, parsed[1], parsed[2], this);
                break;
            case "/x":
                server.broadcastMessage(parsed[1], parsed[2]);
            default:
                sendMessage("ERROR: command not found");
        }
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
