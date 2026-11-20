package com.example.udyamsetubackend.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_customers_contact_key", columnNames = "contact_key"))
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    /** Contact number as entered by the user (trimmed). */
    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    /** Canonical identity used for duplicate detection; see ContactNumberNormalizer. */
    @Column(name = "contact_key", nullable = false, length = 20)
    private String contactKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Required by JPA; not for application use. */
    protected Customer() {
    }

    public Customer(String customerName, String contactNumber, String contactKey, String notes) {
        this.customerName = customerName;
        this.contactNumber = contactNumber;
        this.contactKey = contactKey;
        this.notes = notes;
    }

    /** Replaces the editable details. The contact key must already be normalized by the caller. */
    public void update(String customerName, String contactNumber, String contactKey, String notes) {
        this.customerName = customerName;
        this.contactNumber = contactNumber;
        this.contactKey = contactKey;
        this.notes = notes;
    }

    @PrePersist
    void assignCreationTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getContactKey() {
        return contactKey;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
