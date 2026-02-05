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
}
