package com.example.patient.dto;

import java.util.List;

import lombok.Data;

@Data
public class StrokeDto {
	  private String type;
	    private String color;
	    private Integer lineWidth;
	    private List<PointDto> points;

}
