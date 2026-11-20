package com.example.udyamsetubackend.enquiry.repository;

import com.example.udyamsetubackend.enquiry.entity.Enquiry;
import com.example.udyamsetubackend.enquiry.enums.EnquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long>, JpaSpecificationExecutor<Enquiry> {

    boolean existsByCustomerId(Long customerId);

    List<Enquiry> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    long countByStatus(EnquiryStatus status);

    long countByFollowUpDateAndStatusNotIn(LocalDate date, java.util.List<EnquiryStatus> statuses);

    long countByFollowUpDateBeforeAndStatusNotIn(LocalDate date, java.util.List<EnquiryStatus> statuses);

    /**
     * Demand per catalogue service and status for enquiries created in [from, to). Enquiries are counted
     * against the service they are linked to. {@code recentTotal} counts those created at or after recentFrom.
     */
    @Query("""
            select s.id as serviceId,
                   s.name as serviceName,
                   s.active as active,
                   e.status as status,
                   count(e) as total,
                   sum(case when e.createdAt >= :recentFrom then 1 else 0 end) as recentTotal
            from Enquiry e join e.service s
            where e.createdAt >= :from and e.createdAt < :to
            group by s.id, s.name, s.active, e.status
            """)
    List<LinkedServiceDemandRow> countLinkedServiceDemand(@Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to,
                                                          @Param("recentFrom") LocalDateTime recentFrom);

    /**
     * Demand for free-text requests (enquiries with no catalogue link) per exact trimmed spelling and status.
     * The grouping uses a binary collation, so "Café" and "Cafe" are kept apart. Case and whitespace are
     * normalised in Java.
     */
    @Query(value = """
            select trim(requested_service) collate utf8mb4_bin as rawText,
                   status as status,
                   cast(count(*) as signed) as total,
                   cast(sum(case when created_at >= :recentFrom then 1 else 0 end) as signed) as recentTotal
            from enquiries
            where service_id is null
              and requested_service is not null
              and trim(requested_service) <> ''
              and created_at >= :from and created_at < :to
            group by trim(requested_service) collate utf8mb4_bin, status
            """, nativeQuery = true)
    List<FreeTextDemandRow> countFreeTextDemand(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("recentFrom") LocalDateTime recentFrom);

    /** Earliest creation time of any enquiry, or null when there are none. Used as the all-time lower bound. */
    @Query("select min(e.createdAt) from Enquiry e")
    LocalDateTime findEarliestCreatedAt();

    /** Enquiry count per status for enquiries created in [from, to). */
    @Query("""
            select e.status as status, count(e) as total
            from Enquiry e
            where e.createdAt >= :from and e.createdAt < :to
            group by e.status
            """)
    List<StatusCountRow> countStatusesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Enquiry count per time bucket for enquiries created in [from, to). The format is a MySQL DATE_FORMAT
     * pattern such as {@code %Y-%m-%d} (daily) or {@code %Y-%m} (monthly).
     */
    @Query(value = """
            select date_format(created_at, :format) as bucket,
                   cast(count(*) as signed) as total
            from enquiries
            where created_at >= :from and created_at < :to
            group by date_format(created_at, :format)
            """, nativeQuery = true)
    List<TrendCountRow> countTrend(@Param("format") String format,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
