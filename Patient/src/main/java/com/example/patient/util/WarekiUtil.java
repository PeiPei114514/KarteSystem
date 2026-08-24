package com.example.patient.util;

import java.time.LocalDate;

import com.example.patient.dto.WarekiDto;

public class WarekiUtil {

	//和暦を西暦に変換するメソッド
	public static LocalDate toLocalDate(
			WarekiDto dto) {

		int year;
		switch (dto.getEra()) {
		
		case "R":
			year = 2018 + dto.getYear();
			break;
		case "H":
			year = 1988 + dto.getYear();
			break;
		case "S":
			year = 1925 + dto.getYear();
			break;
		default:
			throw new IllegalArgumentException("元号が不正です");
		}

		return LocalDate.of(year, dto.getMonth(), dto.getDay());
	}

	//和暦を西暦に変換するメソッド
	public static WarekiDto toWareki(LocalDate date) {

		WarekiDto dto = new WarekiDto();

		if (date == null) {
			return dto;
		}

		if (!date.isBefore(LocalDate.of(2019, 5, 1))) {
			dto.setEra("R");
			dto.setYear(date.getYear() - 2018);
		} else if (!date.isBefore(LocalDate.of(1989, 1, 8))) {
			dto.setEra("H");
			dto.setYear(date.getYear() - 1988);
		} else {
			dto.setEra("S");
			dto.setYear(date.getYear() - 1925);
		}

		dto.setMonth(date.getMonthValue());
		dto.setDay(date.getDayOfMonth());

		return dto;
	}
	
	/**
	 * 元号と日付の整合性をチェックする。
	 *
	 * @param dto 和暦DTO
	 * @return エラーがあればメッセージ、正常ならnull
	 */
	public static String checkEra(WarekiDto dto) {

	    if (dto.getEra() == null ||
	        dto.getYear() == null ||
	        dto.getMonth() == null ||
	        dto.getDay() == null) {
	        return null;
	    }

	    LocalDate date = toLocalDate(dto);

	    switch (dto.getEra()) {

	        case "R":
	            if (date.isBefore(LocalDate.of(2019, 5, 1))) {
	                return "令和は2019年5月1日からです。";
	            }
	            break;

	        case "H":
	            if (date.isBefore(LocalDate.of(1989, 1, 8))) {
	                return "平成は1989年1月8日からです。";
	            }
	            break;

	        case "S":
	            if (date.isBefore(LocalDate.of(1926, 12, 25))) {
	                return "昭和は1926年12月25日からです。";
	            }
	            break;
	    }

	    return null;
	}
}
