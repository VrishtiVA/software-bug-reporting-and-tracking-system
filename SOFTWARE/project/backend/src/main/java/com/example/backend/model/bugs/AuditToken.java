package com.example.backend.model.bugs;

/**
 * This class is not an entity, as it will be used dynamically and serialised for storage.
 * This class will actually be used to build audit display content so that it can be parsed safely for display.
 */
public class AuditToken {
    private AuditTokenType type;
    private String value;

    //No-arg constructor required for serialisation ability
    public AuditToken() {}
    public AuditToken(AuditTokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public AuditTokenType getType() {return type;}
    public void setType(AuditTokenType type) {this.type = type;}
    public String getValue() {return value;}
    public void setValue(String value) {this.value = value;}
}
