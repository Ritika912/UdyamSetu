package com.example.udyamsetubackend.dashboard.service;

import com.example.udyamsetubackend.dashboard.dto.DashboardResponse;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregates enquiry metrics for the dashboard view.
 */
@Service
public class DashboardService {

    private static final List<EnquiryStatus> CLOSED_STATUSES = List.of(
            EnquiryStatus.CONVERTED, EnquiryStatus.UNFULFILLED, EnquiryStatus.LOST);

    private final EnquiryRepository enquiryRepository;

    public DashboardService(EnquiryRepository enquiryRepository) {
        this.enquiryRepository = enquiryRepository;
    }

    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();

        long totalEnquiries = enquiryRepository.count();
        long newEnquiries = enquiryRepository.countByStatus(EnquiryStatus.NEW);

        long followUpsDueToday = countFollowUpsDueToday(today);
        long overdueFollowUps = countOverdueFollowUps(today);

        return new DashboardResponse(totalEnquiries, newEnquiries, followUpsDueToday, overdueFollowUps);
    }

    private long countFollowUpsDueToday(LocalDate today) {
        return enquiryRepository.countByFollowUpDateAndStatusNotIn(today, CLOSED_STATUSES);
    }

    private long countOverdueFollowUps(LocalDate today) {
        return enquiryRepository.countByFollowUpDateBeforeAndStatusNotIn(today, CLOSED_STATUSES);
    }
}
