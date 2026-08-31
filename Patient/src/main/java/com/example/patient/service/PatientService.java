package com.example.patient.service;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.example.patient.entity.Patient;
import com.example.patient.repository.PatientRepository;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    //コンストラクタ
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    //全員の年齢を引き上げるメソッド
    @Transactional
    public void incrementAllAge() {
        List<Patient> patients = patientRepository.findAll();
        
        for (Patient patient : patients) {
        	if(patient.getBirth()==null) { //誕生日がない場合は年齢で登録しているため対象となる
        		 patient.incrementAge(); //Patientで定義した一人の年齢を引き上げるメソッド
        	}
        }
        patientRepository.saveAll(patients); //一括で保存
    }
}