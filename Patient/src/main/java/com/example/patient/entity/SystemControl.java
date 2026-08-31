package com.example.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "system_control")
@Data
public class SystemControl {

    @Id
    private Integer id;

    @Column(name = "age_update_year")
    private Integer ageUpdateYear;
}