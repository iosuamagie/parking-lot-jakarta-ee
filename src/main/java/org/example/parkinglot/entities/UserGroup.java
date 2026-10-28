package org.example.parkinglot.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "usergroups")
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String username;

    private String userGroup;

    // JPA needs an empty constructor
    public UserGroup() {
    }

    // Constructor with all fields
    public UserGroup(String username, String userGroup) {
        this.username = username;
        this.userGroup = userGroup;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(String userGroup) {
        this.userGroup = userGroup;
    }
}