package com.example.backend.model.users;

public class NotificationToken {
    private Boolean highlight;
    private String value;

    public NotificationToken() {};
    public NotificationToken(Boolean highlight, String value) {
        this.highlight = highlight;
        this.value = value;
    }

    public Boolean getHighlight() {return highlight;}
    public void setHighlight(Boolean highlight) {this.highlight = highlight;}
    public String getValue() {return value;}
    public void setValue(String value) {this.value = value;}
}
