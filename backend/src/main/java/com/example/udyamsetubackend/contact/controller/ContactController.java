package com.example.udyamsetubackend.contact.controller;

import com.example.udyamsetubackend.contact.dto.ContactRequest;
import com.example.udyamsetubackend.contact.dto.ContactResponse;
import com.example.udyamsetubackend.contact.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public contact form endpoint. Only {@code POST /api/contact} is open to anonymous visitors (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ContactResponse submit(@Valid @RequestBody ContactRequest request) {
        return contactService.submit(request);
    }
}
