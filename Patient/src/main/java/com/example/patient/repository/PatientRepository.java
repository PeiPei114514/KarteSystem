package com.example.patient.repository;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.patient.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient,Integer>{
//	 Optional<Karte> findByNamae(String namae); 
 	   //すべての患者が更新された日付順に並ぶ
	 //  List<Patient> findAllByOrderByUpdateDateDesc();
	   //検索をかけた患者が日付順に並ぶ
	   List<Patient> findByNamaeContainingOrderByUpdateDateDesc(String namae);
	   //最新の患者を４件取得
		List<Patient> findTop4ByOrderByUpdateDateDesc();
	   @Query("""
			    SELECT p
			    FROM Patient p
			    WHERE p.namae LIKE :namae%
			    ORDER BY
			        CASE WHEN p.sex = 'M' THEN 0 ELSE 1 END,
			        p.age ASC
			    """)
			List<Patient> findByNamaeOrderBySexAndAge(@Param("namae") String namae);
	   
	 List<Patient> findByNamaeContaining(String namae);//引数に含まれる文字列を部分検索
	 @Modifying //これをつけないと通常の@Query(select文)だと勘違いされる
	 @Transactional //updateやdeleteをおこなう場合の失敗時のトランザクション(正常に巻き戻す)で必要
	 //カルテや問診票が更新されたときに患者更新日も変える
	 @Query(value = """
			    UPDATE Patient
			    SET update_date = CURRENT_TIMESTAMP
			    WHERE patient_id = :id
			    """, nativeQuery = true)
			void updateDate(@Param("id") Integer id);

	 Page<Patient> findAllByOrderByUpdateDateDesc(Pageable pageable);

	 
}
