package com.example.patient.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.example.patient.entity.SystemControl;
import com.example.patient.repository.SystemControlRepository;

@Service
public class AgeUpdateService {

    private final PatientService patientService;
    private final SystemControlRepository systemControlRepository;

    public AgeUpdateService(
            PatientService patientService,
            SystemControlRepository systemControlRepository) {

        this.patientService = patientService;
        this.systemControlRepository = systemControlRepository;
    }

    //年が変わっていないかどうかチェックするよ
    public void checkAndUpdateAge() {

        // 現在の年
        int currentYear = LocalDate.now().getYear();

        // system_controlから前回更新した年を取得
        SystemControl systemControl =
                systemControlRepository.findById(1).orElseThrow();

        int lastUpdateYear = systemControl.getAgeUpdateYear(); //DBに入っている記録された西暦を獲得

        // 年が変わっていたら年齢を繰り上げる(基本的にこの動作は年に一回しか発生しない)
        if (currentYear > lastUpdateYear) {

            patientService.incrementAllAge(); //全員の年齢を引き上げる

            // 更新した年を保存
            systemControl.setAgeUpdateYear(currentYear);
            systemControlRepository.save(systemControl);
        }
    }
}