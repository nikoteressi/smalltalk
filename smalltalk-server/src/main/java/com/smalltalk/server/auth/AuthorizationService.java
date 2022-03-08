package com.smalltalk.server.auth;

public interface AuthorizationService {
    void start();
    void stop();
    String getNicknameByLoginAndPassword(String login, String password);
    String changeNickname(String oldNick, String newNick);
    void changePassword(String nickname, String oldPassword, String newPassword);
    void createNewUser(String login, String password, String nickname);
    void deleteUser(String nickname);
}
