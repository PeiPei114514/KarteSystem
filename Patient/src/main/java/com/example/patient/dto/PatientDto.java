package com.example.patient.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PatientDto {
    private Integer id;
    private String namae;
    private Integer age;
    private String sex;
    private LocalDateTime updateDate;
}
