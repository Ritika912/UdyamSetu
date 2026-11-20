package com.example.udyamsetubackend.enquiry.entity;

import com.example.udyamsetubackend.catalog.entity.ServiceOffering;
import com.example.udyamsetubackend.customer.entity.Customer;
import com.example.udyamsetubackend.enquiry.enums.EnquirySource;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "enquiries")
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer this enquiry belongs to. Every enquiry has one after the customers migration. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * The catalogue service requested, if it was selected from the catalogue. Null for free-text requests
     * and for enquiries created before services existed. Historical enquiries keep this link even after the
     * service is deactivated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceOffering service;

    /** Name and number as supplied with this enquiry (a snapshot; the customer record is the master). */
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "contact_number", nullable = false, length = 20)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private EnquirySource source;

    @Column(name = "requested_service", length = 150)
    private String requestedService;

    @Column(name = "requirements", columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "occasion", length = 100)
    private String occasion;

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "follow_up_time")
    private LocalTime followUpTime;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnquiryStatus status = EnquiryStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Required by JPA; not for application use. */
    protected Enquiry() {
    }

    public Enquiry(Customer customer, String customerName, String contactNumber) {
        this.customer = customer;
        this.customerName = customerName;
        this.contactNumber = contactNumber;
    }

    @PrePersist
    void assignCreationDefaults() {
        if (status == null) {
            status = EnquiryStatus.NEW;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public ServiceOffering getService() {
        return service;
    }

    public void setService(ServiceOffering service) {
        this.service = service;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public EnquirySource getSource() {
        return source;
    }

    public void setSource(EnquirySource source) {
        this.source = source;
    }

    public String getRequestedService() {
        return requestedService;
    }

    public void setRequestedService(String requestedService) {
        this.requestedService = requestedService;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public LocalTime getFollowUpTime() {
        return followUpTime;
    }

    public void setFollowUpTime(LocalTime followUpTime) {
        this.followUpTime = followUpTime;
    }

    public String getFollowUpNotes() {
        return followUpNotes;
    }

    public void setFollowUpNotes(String followUpNotes) {
        this.followUpNotes = followUpNotes;
    }

    public EnquiryStatus getStatus() {
        return status;
    }

    public void setStatus(EnquiryStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
