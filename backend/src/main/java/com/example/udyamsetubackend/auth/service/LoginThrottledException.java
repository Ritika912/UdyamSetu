package com.example.udyamsetubackend.auth.service;

/** Thrown when a sign-in is refused because too many recent attempts failed. */
public class LoginThrottledException extends RuntimeException {

    public LoginThrottledException() {
        super("Too many failed sign-in attempts. Please try again later.");
    }
}
