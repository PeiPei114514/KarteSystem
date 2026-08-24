package com.example.patient.repository;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.patient.entity.Patient;
import com.example.patient.entity.Quest;

public interface QuestRepository extends JpaRepository<Quest,Integer> {
	List<Quest> findTop8ByPatientOrderByUpdateDateDesc(Patient patient);
	Page<Quest> findByPatient_IdOrderByUpdateDateDesc(   //questがpatientを持っているから、Patient_Idという書き方,持ってないならPatientIdになる
		    Integer patientId,
		    Pageable pageable);
}
