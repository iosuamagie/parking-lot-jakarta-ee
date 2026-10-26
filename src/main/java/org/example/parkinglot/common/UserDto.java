package org.example.parkinglot.common;

import java.util.Collection;
import java.util.Collections;

public class UserDto {
    Long id;
    String username;
    String email;
    private Collection<String> groups;

    public UserDto() {
    }

    public UserDto(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public UserDto(Long id, String username, String email, Collection<String> groups) {
        this(id, username, email);
        this.groups = groups;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
    public Collection<String> getGroups() {
        return groups != null ? groups : Collections.emptyList();
    }
}