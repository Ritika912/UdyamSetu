package com.example.udyamsetubackend.dashboard.service;

import com.example.udyamsetubackend.dashboard.dto.DashboardResponse;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import com.example.udyamsetubackend.enquiry.repository.EnquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EnquiryRepository enquiryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void emptyDatabaseReturnsAllZeroCounts() {
        when(enquiryRepository.count()).thenReturn(0L);
        when(enquiryRepository.countByStatus(EnquiryStatus.NEW)).thenReturn(0L);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0L, response.totalEnquiries());
        assertEquals(0L, response.newEnquiries());
        assertEquals(0L, response.followUpsDueToday());
        assertEquals(0L, response.overdueFollowUps());
    }

    @Test
    void totalAndNewEnquiriesAreCounted() {
        when(enquiryRepository.count()).thenReturn(10L);
        when(enquiryRepository.countByStatus(EnquiryStatus.NEW)).thenReturn(3L);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(10L, response.totalEnquiries());
        assertEquals(3L, response.newEnquiries());
    }

    @Test
    void followUpsDueTodayExcludesClosedStatuses() {
        when(enquiryRepository.count()).thenReturn(5L);
        when(enquiryRepository.countByStatus(EnquiryStatus.NEW)).thenReturn(1L);
        when(enquiryRepository.countByFollowUpDateAndStatusNotIn(eq(LocalDate.now()), anyList())).thenReturn(2L);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(2L, response.followUpsDueToday());
    }

    @Test
    void overdueFollowUpsExcludesClosedStatuses() {
        when(enquiryRepository.count()).thenReturn(8L);
        when(enquiryRepository.countByStatus(EnquiryStatus.NEW)).thenReturn(2L);
        when(enquiryRepository.countByFollowUpDateBeforeAndStatusNotIn(eq(LocalDate.now()), anyList())).thenReturn(3L);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(3L, response.overdueFollowUps());
    }

    @Test
    void closedEnquiryWithFollowUpDateIsNotCounted() {
        when(enquiryRepository.count()).thenReturn(2L);
        when(enquiryRepository.countByStatus(EnquiryStatus.NEW)).thenReturn(0L);
        when(enquiryRepository.countByFollowUpDateAndStatusNotIn(eq(LocalDate.now()), anyList())).thenReturn(0L);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0L, response.followUpsDueToday());
    }

    @Test
    void singleOpenEnquiryCountedOnceNotThreeTimes() {
        when(enquiryRepository.count()).thenReturn(1L);
        when(enquiryRepository.countByStatus(EnquiryStatus.NEW)).thenReturn(1L);
        when(enquiryRepository.countByFollowUpDateAndStatusNotIn(eq(LocalDate.now()), anyList())).thenReturn(1L);

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(1L, response.followUpsDueToday());
    }
}
