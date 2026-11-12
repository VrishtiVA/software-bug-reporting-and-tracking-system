package com.example.backend.dto.responses.users;

public class AuthenticationResponse {

    private String message;
    private String jwtToken;
    private String currentUser;
    private String avatarColour;

    public AuthenticationResponse(String message, String jwtToken, String currentUser, String avatarColour) {
        this.message = message;
        this.jwtToken = jwtToken;
        this.currentUser = currentUser;
        this.avatarColour = avatarColour;
    }
    public AuthenticationResponse() {}

    public String getMessage() {return message;}
    public void setMessage(String message) {this.message = message;}
    public String getJwtToken() {return jwtToken;}
    public void setJwtToken(String jwtToken) {this.jwtToken = jwtToken;}
    public String getCurrentUser() {return currentUser;}
    public void setCurrentUser(String currentUser) {this.currentUser = currentUser;}
    public String getAvatarColour() {return avatarColour;}
    public void setAvatarColour(String avatarColour) {this.avatarColour = avatarColour;}
}
