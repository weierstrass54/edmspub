package com.ckontur.edms.component;

import io.vavr.collection.List;
import io.vavr.collection.Traversable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DummyEmailService {
    public void sendMessage(String email, String body) {
        sendMessage(email, "", body);
    }

    public void sendMessage(String email, String subject, String body) {
        sendMessage(email, subject, body, List.empty());
    }

    public void sendMessage(String email, String subject, String body, byte[] attachment) {
        sendMessage(email, subject, body, List.of(attachment));
    }

    public void sendMessage(String email, String subject, String body, Traversable<byte[]> attachments) {
        sendMessage(List.of(email), subject, body, attachments);
    }

    public void sendMessage(Traversable<String> emails, String body) {
        sendMessage(emails, "", body);
    }

    public void sendMessage(Traversable<String> emails, String subject, String body) {
        sendMessage(emails, subject, body, List.empty());
    }

    public void sendMessage(Traversable<String> emails, String subject, String body, byte[] attachment) {
        sendMessage(emails, subject, body, List.of(attachment));
    }

    public void sendMessage(Traversable<String> emails, String subject, String body, Traversable<byte[]> attachments) {
        emails.forEach(
            email -> log.info("Sent email on address {} with subject {}, body {} and {} attachments", email, subject, body, attachments.size())
        );
    }

}
