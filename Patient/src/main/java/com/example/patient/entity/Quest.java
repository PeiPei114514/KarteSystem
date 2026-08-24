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
@Table(name="Quest")
@Data
public class Quest {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="quest_id")
	private Integer id;
	@JoinColumn(name = "patient_id")
	@ManyToOne
	private Patient patient; //外部キー
	@Column(name="img_file_path")
	private String imgFilePath;
	
	@Column(
		    name = "update_date",
		    insertable = false,
		    updatable = false
		)
	private LocalDateTime updateDate;
	

}
