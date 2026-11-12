package com.example.backend.model.users;

import jakarta.persistence.*;

@Entity
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column
    private String colour;
    @Column
    private String role;

    protected User() {}
    public User(String username, String email, String passwordHash, String colour, String role) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.colour = colour;
        this.role = role;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}
    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}
    public String getPasswordHash() {return passwordHash;}
    public void setPasswordHash(String passwordHash) {this.passwordHash = passwordHash;}
    public String getColour() {return colour;}
    public void setColour(String colour) {this.colour = colour;}
    public String getRole() {return role;}
    public void setRole(String role) {this.role = role;}
}
