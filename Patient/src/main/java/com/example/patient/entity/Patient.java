package com.example.patient.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Entity
@Table(name="Patient")
@Data
public class Patient {
@Id
@GeneratedValue(strategy=GenerationType.IDENTITY)
@Column(name="patient_id")
private Integer id;
@NotBlank(message = "氏名を入力してください")
private String namae;
private LocalDate birth;
private Integer age;
private String sex; //性別男「M」または女「F」が入力される

@OneToMany(mappedBy ="patient" ,cascade=CascadeType.ALL)
private List<Karte> KarteList =new ArrayList<>();

@OneToMany(mappedBy ="patient" ,cascade=CascadeType.ALL)
private List<Quest> questList =new ArrayList<>();
@Column(
	    name = "update_date",
	    insertable = false,
	    updatable = false
	)
private LocalDateTime updateDate;

//なぜかidのゲッターだけ作られていない
public Integer getId() {
	return id;
}


//年齢取得メソッド
public Integer getAge() {
	if(birth==null) {
		return age;
	}else {
			LocalDate today =LocalDate.now(); //今日
	int age= today.getYear()-birth.getYear();
	if(today.getMonthValue()<birth.getMonthValue()||
			today.getMonthValue()==birth.getMonthValue()&&
			today.getDayOfMonth()< birth.getDayOfMonth()) {
		//現在の月が生まれ月よりも小さい場合,または
		 //現在の月が生まれ月と同じかつ、現在の日のほうが若い場合
		age-=1;
	}	
	return age;
	}
}


}
