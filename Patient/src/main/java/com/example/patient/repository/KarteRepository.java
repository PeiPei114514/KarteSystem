package com.example.patient.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.patient.entity.Karte;
import com.example.patient.entity.Patient;

public interface KarteRepository extends JpaRepository<Karte,Integer>{
	List<Karte> findTop8ByPatientOrderByUpdateDateDesc(Patient patient);
	Page<Karte> findByPatient_IdOrderByUpdateDateDesc(
		    Integer patientId,
		    Pageable pageable);

}
