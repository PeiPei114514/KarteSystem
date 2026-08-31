package com.example.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.patient.entity.SystemControl;

public interface SystemControlRepository
        extends JpaRepository<SystemControl, Integer> {

}