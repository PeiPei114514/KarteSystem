package com.example.patient.dto;

import java.util.List;

import lombok.Data;

@Data
public class DrawingDto {

	
    private Integer patientId;
    private Integer karteId;
    private int width;
    private int height;
    private List<StrokeDto> strokes; // List<List<PointDto>> から変更

}