package com.example.backend.services.users;

import com.example.backend.model.users.NotificationToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notification message builder implemented using a builder pattern,
 * allowing chaining of methods to build a message composed of notification tokens.
 * Useful for later formatting message contents on the frontend.
 */
public class NotificationMessageBuilder {

    /** Initial object has an empty notification message. */
    private final List<NotificationToken> notificationMessage = new ArrayList<>();

    /** Method to add text token to notification message. */
    public NotificationMessageBuilder text(String text) {
        this.notificationMessage.add(new NotificationToken(false, text));
        return this;
    }

    /** Method to add highlighted item to notification message. */
    public NotificationMessageBuilder highlight(String text) {
        this.notificationMessage.add(new NotificationToken(true, text));
        return this;
    }

    /**
     * Turn the notification message into an immutable list of NotificationTokens
     * @return an immutable list of NotificationTokens
     */
    public List<NotificationToken> build() {
        return Collections.unmodifiableList(this.notificationMessage);
    }

}
