package com.example.patient.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name="Karte")
@Data
public class Karte {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="karte_id")
	private Integer id;
	@JoinColumn(name="patient_id")
	@ManyToOne
	private Patient patient; //外部キー
	@Column(name="drawJson",columnDefinition = "LONGTEXT")
	private String drawJson;
	private String imgFilePath;
	@Column(
		    name = "update_date",
		    insertable = false,
		    updatable = false
		)
	private LocalDateTime updateDate;
	

}
