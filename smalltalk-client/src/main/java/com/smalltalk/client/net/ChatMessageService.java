package com.smalltalk.client.net;

import java.io.IOException;

public class ChatMessageService {
    private MessageProcessor messageProcessor;
    private ChatNetworkService chatNetworkService;

    public ChatMessageService(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    public void connect() {
        if (isConnected()) return;
        try {
            this.chatNetworkService = new ChatNetworkService(this);
            chatNetworkService.readMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return this.chatNetworkService != null && !this.chatNetworkService.getSocket().isClosed();
    }

    public void send(String message) {
        this.chatNetworkService.sendMessage(message);
    }

    public void receive(String message) {
        messageProcessor.processMessage(message);
    }

}
