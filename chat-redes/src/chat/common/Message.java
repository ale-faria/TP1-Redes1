package chat.common;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sender;
    private final String text;
    private final String timestamp;

    public Message(String sender, String text) {
        this.sender = sender;
        this.text = text;
        this.timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getSender()    { return sender; }
    public String getText()      { return text; }
    public String getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender + ": " + text;
    }
}
