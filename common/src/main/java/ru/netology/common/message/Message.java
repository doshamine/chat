package ru.netology.common.message;

import java.time.LocalDateTime;

public class Message {
    private final String username;
    private final LocalDateTime createdAt;
    private String text;

    public Message(String username, String text) {
        this.username = username;
        this.createdAt = LocalDateTime.now();
        this.text = text;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
