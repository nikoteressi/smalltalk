package com.smalltalk.client.app;

import com.smalltalk.client.net.ChatMessageService;
import com.smalltalk.client.net.MessageProcessor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements MessageProcessor {


    public VBox loginPanel;
    public TextField loginField;
    public PasswordField passwordField;
    private ChatMessageService chatMessageService;
    private String nickName;
    public VBox mainChatPanel;
    public TextArea mainChatArea;
    public ListView<String> contactList;
    public TextField inputField;
    public Button btnSendMessage;


    public void exit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void sendMessage(ActionEvent actionEvent) {
        String text = inputField.getText();
        if (text.isEmpty()) return;
        chatMessageService.send(this.nickName + ": " + text);
        inputField.clear();
    }


    @Override
    public void processMessage(String message) {
        Platform.runLater(() -> parseMessage(message));
    }

    public void sendAuth(ActionEvent actionEvent) {
        if (loginField.getText().isBlank() || passwordField.getText().isBlank()) return;
        chatMessageService.connect();
        chatMessageService.send("auth: " + loginField.getText() + " " + passwordField.getText());
    }

    private void parseMessage(String message) {
        if (message.startsWith("authok: ")) {
            this.nickName = message.substring(8);
            loginPanel.setVisible(false);
            mainChatPanel.setVisible(true);
        } else if (message.startsWith("ERROR: ")) {
            showError(message);
        } else if (message.startsWith("$.list: ")) {
            ObservableList<String> list = FXCollections.observableArrayList(message.substring(8).split("\\s"));
            contactList.setItems(list);
        } else {
            mainChatArea.appendText(message + System.lineSeparator());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ERROR");
        alert.setHeaderText(message);

        alert.showAndWait();

    }

    public void mockAction(ActionEvent actionEvent) {
    }
}
