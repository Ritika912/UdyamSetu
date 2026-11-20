package com.example.udyamsetubackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Owner sign-in request. The password is never logged or echoed back, and {@link #toString()} masks it.
 */
public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 200) String password
) {

    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=********]";
    }
}
