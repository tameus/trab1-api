package sd2526.trab.server.java;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class DeletedMessage {
    @Id
    private String messageId;

    public DeletedMessage() {}
    public DeletedMessage(String messageId) { this.messageId = messageId; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
}
