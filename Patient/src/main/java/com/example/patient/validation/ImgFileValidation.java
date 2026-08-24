package com.example.patient.validation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.patient.service.MedicalFormImageProcessingService;


@Component
public class ImgFileValidation {
	
//   	 @Autowired
//	    private A4DocumentImageProcessor processor;
	@Autowired
	private MedicalFormImageProcessingService processor;
	
	//ファイルが不正なら例外を発生させる
	//戻り値　：　読みこんだファイルの拡張子を返却する
 	public  String validation(MultipartFile file) throws IOException{
	   String originalName = file.getOriginalFilename();	
	    String extension = originalName.substring(originalName.lastIndexOf("."));

	    try (InputStream is = file.getInputStream()) {
	        BufferedImage image = ImageIO.read(is); //1バイトずつデータ解析をして不正がないかチェックしている
        if (image == null) {
	        throw new IllegalArgumentException("画像ではありません");
	    }
	    }
	  //識別子が正しいかどうか判定
	    if (!extension.equals(".png") &&
	    	    !extension.equals(".jpg") &&
	    	    !extension.equals(".jpeg")) {
	    	    throw new IllegalArgumentException("不正な形式"); //IllegalArgumentExceptionとはユーザの入力が間違っていた時の例外
	    	}
	    if(!file.getContentType().startsWith("image/")) { //startsWithは"image/"で始まるならtureを返す
	    	    throw new IllegalArgumentException("画像ではありません");
	    	}    
	    UUID uuid =UUID.randomUUID();//ランダムな値を作る
	    String uuidName =uuid.toString();
	    String fileName=uuidName+extension;
	    Path path = Paths.get("Z:/upload/" + fileName);//Pathsで文字列をpath型に変換している
	  
	  //  file.transferTo(path); //ここで初めて,メモリ上にのみあったimgファイルがuploadフォルダに書き込まれる
     
	    
//	    byte[] processed = processor.process(file.getBytes());
//	    Files.write(path, processed);
	    byte[] processed = processor.process(file.getBytes());
	    Files.write(path, processed);
	    
	    return fileName;//ファイル名を返却
	}
}
