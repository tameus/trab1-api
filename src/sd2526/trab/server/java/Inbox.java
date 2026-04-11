package sd2526.trab.server.java;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Inbox {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String userAddress;
    private String messageId;

    public Inbox() {}
    public Inbox(String userAddress, String messageId) {
        this.userAddress = userAddress;
        this.messageId = messageId;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUserAddress() {
        return userAddress;
    }
    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }
    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
