package com.example.patient.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AgeUpdateRunner implements CommandLineRunner {

    private final AgeUpdateService ageUpdateService;

    public AgeUpdateRunner(AgeUpdateService ageUpdateService) {
        this.ageUpdateService = ageUpdateService;
    }

    @Override
    public void run(String... args) {

        ageUpdateService.checkAndUpdateAge();

    }
}