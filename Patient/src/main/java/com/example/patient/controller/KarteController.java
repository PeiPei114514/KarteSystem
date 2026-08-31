package com.example.patient.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.patient.dto.DrawingDto;
import com.example.patient.dto.PatientDto;
import com.example.patient.dto.WarekiDto;
import com.example.patient.entity.Karte;
import com.example.patient.entity.Patient;
import com.example.patient.entity.Quest;
import com.example.patient.repository.KarteRepository;
import com.example.patient.repository.PatientRepository;
import com.example.patient.repository.QuestRepository;
import com.example.patient.util.WarekiUtil;
import com.example.patient.validation.ImgFileValidation;

import lombok.AllArgsConstructor;
import tools.jackson.databind.ObjectMapper;



@Controller
@AllArgsConstructor
public class KarteController {
	private final PatientRepository patientRepository;
	private final KarteRepository karteRepository;
	private final QuestRepository questRepository;
	
	@Autowired
	private ImgFileValidation imgFileValidation;
	
	private final String filePath = "Z:/upload/"; //カルテや問診票を保存するためのフォルダパス
	
	
	//トップに戻るボタンを押されたときの処理
	@GetMapping("/top_back")
	public String topButton() {
		
		return "redirect:/show_patient";
	}
	

//	public String backSearch() {
//		return "redirect:/select_form";
//	}
	//	//サーチ画面に戻る処理
	@GetMapping("/back_search")
	public ModelAndView backSearch(
	        @RequestParam(required = false) String namae,
	        @RequestParam(defaultValue = "0") int page) {
		
		//戻り値は自作の検索メソッド。そいつに処理を任せている
	    return selectPatient(page, namae, new ModelAndView());
	}
	
	
	@GetMapping("/show_patient")
	public ModelAndView showPatient(ModelAndView mv) {
	
		List<Patient> patientList =patientRepository.findTop4ByOrderByUpdateDateDesc();
		mv.setViewName("show_patient");
		mv.addObject("patientList",patientList);
		return mv;
	}
	
	
	// ログイン画面を表示する
	@PreAuthorize("permitAll")
	@GetMapping("/login")
	public ModelAndView login(ModelAndView mv, String error) {
		

		mv.setViewName("login");
		if (error != null) {
			mv.addObject("msg", "ログインできませんでした。");
		} else {
			mv.addObject("msg", "ユーザー名とパスワードを入力:");
		}
		return mv;
	}

	//個人ページ
	@GetMapping("/detail/{id}")
	public ModelAndView detailPatient(@PathVariable int id,
			@RequestParam(defaultValue="top") String from,
			ModelAndView mv) {
		Patient patient=patientRepository.findById(id).get();
		//横スクロールとして表示するため
		List<Karte> latestKarte =
		        karteRepository.findTop8ByPatientOrderByUpdateDateDesc(
		                patient);

		List<Quest> latestQuest =
		        questRepository.findTop8ByPatientOrderByUpdateDateDesc(
		                patient);
		
		WarekiDto dto = WarekiUtil.toWareki(patient.getBirth());
		mv.addObject("wareki", dto);
		mv.addObject("latestKarte", latestKarte);
		mv.addObject("latestQuest", latestQuest);
		mv.addObject("from",from);
		
	
		mv.addObject("patient",patient);
	    mv.setViewName("detail");
		return mv;
	}
	
	//検索したとき
	@GetMapping("/detail")
	public ModelAndView selectPatient(
			 @RequestParam(defaultValue = "0") int page,
			 @RequestParam(required = false) String namae, //@RequestParam(value = "namae", required = false)の省略
		          //namaeが来なくてもエラーにならずにnamae=nullとなる
			 ModelAndView mv) {
		 Pageable pageable = PageRequest.of(page, 20);

		    Page<Patient> patientPage;
		           

		
		List<Patient> patientList;
		
	    if (namae == null || namae.isBlank()) { //nullか空欄のとき
	    	patientPage =  patientRepository.findAllByOrderByUpdateDateDesc(pageable);
	    	 mv.addObject("patientList", patientPage.getContent());
	 	    mv.addObject("page", page);
	 	    mv.addObject("hasNext", patientPage.hasNext());
	 	
	    } else {
	    	patientList = patientRepository.findByNamaeOrderBySexAndAge(namae);
	    	mv.addObject("patientList",patientList);
	    
	    }

	   
		mv.setViewName("select_form");
//		mv.addObject("patientList",patientList);
		return mv;
	}
	
	//Ajaxでもっと見るを追加する
	@GetMapping("/patient/list")
	@ResponseBody
	public List<PatientDto> patientList(
	        @RequestParam int offset,
	        @RequestParam(defaultValue = "20") int limit) {

	    Pageable pageable = PageRequest.of(offset / limit, limit);

	    return patientRepository
	            .findAllByOrderByUpdateDateDesc(pageable)
	            .getContent()
	            .stream()
	            .map(p -> {
	                PatientDto dto = new PatientDto();
	                dto.setId(p.getId());
	                dto.setNamae(p.getNamae());
	                dto.setAge(p.getAge());
	                dto.setSex(p.getSex());
	                dto.setUpdateDate(p.getUpdateDate());
	                return dto;
	            })
	            .toList();
	}
	
//	//[問診票なし]で患者を追加する
//	@GetMapping("/insert_form")
//	public String insertForm(Model model) {
//		model.addAttribute("patient", new Patient()); //空のインスタンスを渡す
//		return "insert_form";	
//	}
	

	
	//[問診票あり]で患者を追加する画面表示
	@GetMapping("/insert_form")
	public String insertForm(Model model) {
	    model.addAttribute("patient", new Patient()); //空のインスタンスを渡す
	    model.addAttribute("wareki", new WarekiDto()); //空の和暦インスタンスを渡さないとダメ
	    return "insert_form";
	}
	
//	//問診票から追加で写真をアップロードしたとき、一時的にsessionにファイルを入れる
//		@PostMapping("/insert_form_quest_ver")
//		public String uploadFileQuest(
//		        @RequestParam("img_file") MultipartFile file,
//		        HttpSession session) throws IOException, ImageProcessingException {
//			
//			
//		    // ファイル保存
//		    String fileName = imgFileValidation.validation(file);
//		    // セッションに保存
//		    session.setAttribute("questImage", fileName);
//		    // 入力画面へ
//		    return "redirect:/insert_form";
//		}
//		
		/*	//問診票から追加ページで画像をサーバから削除する
		@PostMapping("/delete_quest_image")
		public String deleteQuestImage( HttpSession session) {
	
		    // セッションから questImage を削除
		    session.removeAttribute("questImage");
	
		    return "redirect:/insert_form";
		}
		*/

	//新規と更新データをDBに保存する
	@PostMapping("/insert_patient")
	public String insertPatient(@Valid @ModelAttribute Patient patient,
			BindingResult result,
			 @ModelAttribute WarekiDto wareki,

		        @RequestParam String ageType,

		        @RequestParam(value = "img_file", required = false) MultipartFile imgFile,
			 Model model) throws IOException {

		//名前が未記入かどうかをチェック
	    if (result.hasErrors()) {
	        model.addAttribute("wareki", wareki);
	        model.addAttribute("ageType",ageType);
	        return "/insert_form";
	    }
	    
	    if(ageType.equals("wareki")) {
	    //和暦が途中でとぎきれてないかチェック
	    if (ageType.equals("wareki")) {
	        if (wareki.getYear() == null ||
	            wareki.getMonth() == null ||
	            wareki.getDay() == null) {

	            model.addAttribute("warekiError", "生年月日は年月日をすべて入力してください");
	            model.addAttribute("wareki", wareki);
	            model.addAttribute("ageType",ageType);
	            return "insert_form";
	        }
	    }
	    //存在しない元号の日付入力を禁止する(令和１年１月1日など)
	    String error = WarekiUtil.checkEra(wareki); //エラーメッセージを作る
	    if (error != null) {
	        model.addAttribute("warekiError", error);
	        model.addAttribute("wareki", wareki);
	        model.addAttribute("ageType",ageType);
	        return "insert_form";
	    }
	    //和暦を西暦に変換
	    LocalDate birth = WarekiUtil.toLocalDate(wareki);
		
		//未来の日付の入力を禁止する
	    if (birth != null && birth.isAfter(LocalDate.now())) {
	        model.addAttribute("warekiError", "未来の日付は入力できません。");
	        model.addAttribute("wareki", wareki);
	        model.addAttribute("ageType",ageType);
	        return "insert_form";
	    }
	    patient.setBirth(birth); //誕生日をセット
	    }
		patientRepository.saveAndFlush(patient);
		//問診票から入力時のファイル保存

		  
		  

		    // 問診票画像の保存 ← ここを丸ごと差し替え
		    if (imgFile != null && !imgFile.isEmpty()) {
		        String fileName = imgFileValidation.validation(imgFile); // ここで初めてディスク保存
		        Quest quest = new Quest();
		        quest.setPatient(patient);
		        quest.setImgFilePath(fileName);
		        questRepository.save(quest);
		    }
		  
		return "redirect:/show_patient";
	}
	

	
	//患者の更新データをhtmlに飛ばし、デフォルト値に設定する
	@GetMapping("/update_patient/{id}")
	public ModelAndView updatePatient(@PathVariable int id,
			ModelAndView mv) {
		Patient patient =patientRepository.findById(id).orElseThrow();
				WarekiDto dto = WarekiUtil.toWareki(patient.getBirth());
				mv.addObject("wareki", dto);
		
		String ageType;
		//何もないときはnullを入れる
				if(patient.getBirth()==null && patient.getAge()==null) {
					ageType=null;
					//誕生日が無いときはageを設定
				}else if(patient.getBirth()==null) {
					ageType="age";
				}else {
					ageType="wareki";
				}
		
		mv.addObject("ageType", ageType);
		mv.setViewName("insert_form");
		mv.addObject("patient",patient);
		
		return mv;
	}
	
	@GetMapping("/select_form")
	public String selectForm() {
	    return "select_form";
	}
	
	//患者を削除する
	@GetMapping("/delete_patient/{id}")
	public String daletePatient(@PathVariable int id) throws IOException{		
	   Patient patient = patientRepository.findById(id).orElseThrow();
	   //削除する患者に対応するカルテ画像を削除する
	   for(Karte karte :patient.getKarteList()) {
		   if(karte.getImgFilePath()!=null) { //画像ファイルが存在する場合のみ(JSONはスルー)
			       Path path = Paths.get(filePath+karte.getImgFilePath());
		    Files.deleteIfExists(path);
		   }	
	   }
	   //問診票を削除
	   for(Quest quest :patient.getQuestList()) {
			Path path = Paths.get(filePath+quest.getImgFilePath());
		    Files.deleteIfExists(path);		   	
	   }
	   //データベースのレコードを削除
	   patientRepository.delete(patient);
		return "redirect:/show_patient";
	}
	
//	@GetMapping("/show_karte")
//	public ModelAndView showKarte(ModelAndView mv){
//		mv.setViewName("show_karte"); 
//		return mv;
//	}
	
	@GetMapping("/show_karte/{id}")
	public ModelAndView showKarte(@PathVariable int id,
			 @RequestParam(defaultValue = "0") int page,
			ModelAndView mv) {
	    Patient patient = patientRepository.findById(id).orElseThrow(); //これでリポジトリから引数でもらったインスタンスを貰う
	
	    Pageable pageable = PageRequest.of(page, 12);

	    Page<Karte> questPage =
	            karteRepository.findByPatient_IdOrderByUpdateDateDesc(id, pageable);

	    
	    mv.addObject("patient",patient);
	    mv.addObject("questPage", questPage);
	    mv.addObject("currentPage", page);
	    mv.setViewName("show_karte");
	    return mv;
	}
	
	//問診票一覧を表示
	@GetMapping("/show_quest/{id}")
	public ModelAndView showQuest(@PathVariable int id,
			 @RequestParam(defaultValue = "0") int page,
			ModelAndView mv) {
	    Patient patient = patientRepository.findById(id).orElseThrow();
	   
	    Pageable pageable = PageRequest.of(page, 12);

	    Page<Quest> questPage =
	            questRepository.findByPatient_IdOrderByUpdateDateDesc(id, pageable);

	    
	    mv.addObject("patient",patient);
	    
	    mv.addObject("questPage", questPage);
	    mv.addObject("currentPage", page);
	    mv.setViewName("show_quest");
	    return mv;
	}
	
	
	//カルテ：問診表ページの戻るボタン
	@PostMapping("/back_show_patient/{id}")
	public String backShowKarte(@PathVariable int id,
			@RequestParam int page,
			@RequestParam String type) {
		   if (page > 0) {
		        return "redirect:/show_"+type+"/" + id + "?page=" + (page - 1);
		    }
		return "redirect:/detail/"+id;
	}
	
	
	@GetMapping("/show_karte_detail/{id}")
	public ModelAndView showKarteDetail(@PathVariable int id,
			ModelAndView mv) {
		Karte karte = karteRepository.findById(id).orElseThrow();
		mv.addObject("karte",karte);
		mv.setViewName("show_karte_detail");
		return mv;
	}
	
	
	@GetMapping("/show_quest_detail/{id}")
	public ModelAndView showquestDetail(@PathVariable int id,
			ModelAndView mv) {
		Quest quest =questRepository.findById(id).orElseThrow();
		mv.addObject("quest",quest);
		mv.setViewName("show_quest_detail");
		return mv;
	}
	
	//_____________カルテand問診票の編集系____________
	
	//カルテを削除
	//問題点　deleteしたはいいが、戻るボタンで二重削除されてしまう(キャッシュの問題でどうしようもない) 
	@PostMapping("/delete_karte")
	public String deleteKarte(@RequestParam int id) throws IOException{
		Karte karte = karteRepository.findById(id).orElseThrow();
		int patientId =karte.getPatient().getId();
	    Path path = Paths.get(filePath+karte.getImgFilePath());
	    Files.deleteIfExists(path);
	    patientRepository.updateDate(patientId);
		karteRepository.delete(karte);
		return "redirect:/show_karte/"+patientId;
	}
	
	//問診票を削除
	@PostMapping("/delete_quest")
	public String deleteQuest(@RequestParam int id) throws IOException{
		Quest quest = questRepository.findById(id).orElseThrow();
		int patientId =quest.getPatient().getId();
	    Path path = Paths.get(filePath+quest.getImgFilePath());
	    Files.deleteIfExists(path);
	    patientRepository.updateDate(patientId);
		questRepository.delete(quest);
		return "redirect:/show_quest/"+patientId;
	}
	
	//________画像系統_________
	
	//カルテ画像新規追加
	@PostMapping("/new_upload_karte")
	public String uploadFile(
	        @RequestParam("img_file") MultipartFile file,
	        @RequestParam("id") Integer patientId)
	        throws IOException{
		String fileName= imgFileValidation.validation(file);//例外チェックとファイル名を取得
	    Patient patient = patientRepository.findById(patientId)
	            .orElseThrow(); //こいつはget()の上位互換で、値が不明だと例外を発生させる 
	    //optional型のpatientリストが返却される
	    Karte karte =new Karte();
	    karte.setPatient(patient);
	    karte.setImgFilePath(fileName);
	    patientRepository.updateDate(patientId);
	    karteRepository.save(karte);
	    return "redirect:/show_karte/" + patientId; 
	    }
	
	
	//問診表画像追加
	@PostMapping("/new_upload_quest")
	public String uploadFileQuest(
			@RequestParam("img_file") MultipartFile file,
			@RequestParam("id") Integer patientId)
					throws IOException  {
		String fileName= imgFileValidation.validation(file);//例外チェックとファイル名を取得
//		Patient patient = new Patient(); //なぜnewしているかと言うと新規で追加されるときにダミーのpatientを事前に作って置くためだが、この書き方だと汎用性が無いため、あくまでテストのため
		Patient patient = patientRepository.findById(patientId).orElseThrow();
		Quest quest = new Quest();
		quest.setPatient(patient);
		quest.setImgFilePath(fileName);
		 patientRepository.updateDate(patientId);
		questRepository.save(quest);
		  return "redirect:/show_quest/"+patientId; 
	}
	
	
	//カルテ 画像ファイル更新
	@PostMapping("/update_upload_karte")
	public String updateUploadFileKarte(   
			@RequestParam("img_file") MultipartFile file,
	        @RequestParam("id") Integer karteId) 
	        		throws IOException {
		 Karte karte =karteRepository.findById(karteId).orElseThrow();
		 patientRepository.updateDate(karte.getPatient().getId());
		 //古い画像をフォルダから削除する
	    Path path = Paths.get(filePath+karte.getImgFilePath());
	    Files.deleteIfExists(path);
	    //例外チェックとファイル名を取得
		String fileName= imgFileValidation.validation(file);
			    karte.setImgFilePath(fileName);
			    karteRepository.saveAndFlush(karte);
			    int patientId =karte.getPatient().getId();
		 return "redirect:/show_karte/"+patientId;
	}
	
	
	//問診票 画像ファイル更新
	@PostMapping("/update_upload_quest")
	public String updateUploadFileQuest(   
			@RequestParam("img_file") MultipartFile file,
	        @RequestParam("id") Integer questId) 
	        		throws IOException{
		
		 Quest quest =questRepository.findById(questId).orElseThrow();
		 patientRepository.updateDate(quest.getPatient().getId());
//		 Patient patient= quest.getPatient();
//		 patient.setId(patient.getId());
//		 patientRepository.saveAndFlush(patient);
		 //古い画像をフォルダから削除する
	    Path path = Paths.get(filePath+quest.getImgFilePath());
	    Files.deleteIfExists(path);
	    //例外チェックとファイル名を取得
		String fileName= imgFileValidation.validation(file);
			    quest.setImgFilePath(fileName);
			    questRepository.saveAndFlush(quest);
			    int patientId =quest.getPatient().getId();
		 return "redirect:/show_quest/"+patientId;
	}

	//sqlのファイル名からブラウザにパスを知らせるためのマッピングクラス
	@Configuration
	 class WebConfig implements WebMvcConfigurer {

	    @Override
	    public void addResourceHandlers(ResourceHandlerRegistry registry) {
	        registry.addResourceHandler("/images/**")
	                .addResourceLocations("file:"+filePath); //ここでパスを翻訳している
	    }
	}
	
	
	//_______手書きカルテ系統_______
	
	//新規手書きカルテ
	@GetMapping("upload_drawing/{id}")
	public String drawing(@PathVariable int id,Model model) {
		  model.addAttribute("patientId", id);//Modelクラスでデータのみを送信
		  model.addAttribute("drawingData", null); //JSON文字列（新規なのでnull）
		return "drawing";//ModelAndView.setViewName("drawing")に相当する
	}
	


	//手書きカルテ（JSON）をDBに保存する
	    @ResponseBody
	    @PostMapping("/saveDrawing")
	    public String saveDrawing(@RequestBody DrawingDto dto) {
	        ObjectMapper mapper = new ObjectMapper();
	        String json = mapper.writeValueAsString(dto);//dtoをstring(jsonに変換する)
//	        System.out.print(json);    
	        Karte karte =new Karte();
	        Patient patient;
	        patient = patientRepository.findById(dto.getPatientId()).orElseThrow();
	        if(dto.getKarteId()!=null) { //更新時
	        	karte =karteRepository.findById(dto.getKarteId()).orElseThrow();
	        }else { //新規
	             
	            karte.setPatient(patient);
	         }
	        karte.setDrawJson(json);
	        karteRepository.save(karte);    
	        return "/show_karte/"+patient.getId();
	}
	    
	    //手書きカルテを表示させ、編集可能にする
	    @GetMapping("/viewer/{id}")
	    public ModelAndView ShowViewer(@PathVariable int id,
	    		ModelAndView mv) {
//	    Karte karte =patientRepository.findById(1).get().getKarteList().get(1);
	    Karte karte =karteRepository.findById(id).orElseThrow();
	    mv.addObject("drawingData", karte.getDrawJson());//JSON文字列を送る
	    mv.addObject("patientId", karte.getPatient().getId());
	    mv.addObject("karteId", karte.getId());
	    mv.setViewName("drawing");
	    return mv;
	    }
	    
	    //手書きカルテを削除する
	    @PostMapping("/delete-json")
	    public String dereteJson(@RequestParam int id) {
			Karte karte = karteRepository.findById(id).orElseThrow();
			int patientId =karte.getPatient().getId();

		    patientRepository.updateDate(patientId);
			karteRepository.delete(karte);
			return "redirect:/show_karte/"+patientId;
	    }
		
}
