package com.example.udyamsetubackend.customer.repository;

import com.example.udyamsetubackend.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByContactKey(String contactKey);

    List<Customer> findAllByOrderByCreatedAtDescIdDesc();
}
