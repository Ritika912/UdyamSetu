package com.example.udyamsetubackend.enquiry.controller;

import com.example.udyamsetubackend.enquiry.dto.EnquiryRequest;
import com.example.udyamsetubackend.enquiry.dto.EnquiryResponse;
import com.example.udyamsetubackend.enquiry.dto.EnquiryStatusRequest;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.service.EnquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnquiryResponse createEnquiry(@Valid @RequestBody EnquiryRequest request) {
        return enquiryService.createEnquiry(request);
    }

    @GetMapping
    public List<EnquiryResponse> listEnquiries(
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(required = false) Long customerId) {
        return enquiryService.listEnquiries(status, customerId);
    }

    @GetMapping("/{id}")
    public EnquiryResponse getEnquiry(@PathVariable Long id) {
        return enquiryService.getEnquiry(id);
    }

    @PatchMapping("/{id}/status")
    public EnquiryResponse updateStatus(@PathVariable Long id, @Valid @RequestBody EnquiryStatusRequest request) {
        return enquiryService.updateStatus(id, request.status());
    }
}
