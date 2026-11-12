package com.example.backend.services.bugs;

import com.example.backend.model.bugs.AuditToken;
import com.example.backend.model.bugs.AuditTokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A utility class for constructing an audit message as a List of Audit Tokens by chaining methods.
 * Reading on builder patterns: <a href="https://medium.com/@alxkm/builder-pattern-variations-and-best-practices-643b6631341f">Builder Patterns</a>
 */
public class AuditMessageBuilder {

    /** Initial object has an empty audit message. */
    private final List<AuditToken> auditMessage = new ArrayList<>();

    /** Add a TEXT token to the audit message. */
    public AuditMessageBuilder text(String text) {
        this.auditMessage.add(new AuditToken(AuditTokenType.TEXT, text));
        return this;
    }
    /** Add a USERNAME token to the audit message. */
    public AuditMessageBuilder username(String username) {
        this.auditMessage.add(new AuditToken(AuditTokenType.USERNAME, username));
        return this;
    }
    /** Add an OLD_VALUE token to the audit message. */
    public AuditMessageBuilder oldValue(String oldValue) {
        this.auditMessage.add(new AuditToken(AuditTokenType.OLD_VALUE, oldValue));
        return this;
    }
    /** Add a NEW_VALUE token to the audit message. */
    public AuditMessageBuilder newValue(String newValue) {
        this.auditMessage.add(new AuditToken(AuditTokenType.NEW_VALUE, newValue));
        return this;
    }
    /** Add an ID token to the audit message. */
    public AuditMessageBuilder id(Long id) {
        this.auditMessage.add(new AuditToken(AuditTokenType.ID, id.toString()));
        return this;
    }
    /**
     * Turn the audit message into an immutable list of AuditTokens
     * @return an immutable list of AuditTokens
     */
    public List<AuditToken> build() {
        return Collections.unmodifiableList(this.auditMessage);
    }
}
