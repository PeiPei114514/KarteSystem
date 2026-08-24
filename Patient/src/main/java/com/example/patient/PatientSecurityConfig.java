package com.example.patient;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import lombok.AllArgsConstructor;

@Configuration // アプリケーション実行時にこのクラスの Bean が実行されインスタンスが作られる
@EnableMethodSecurity // コントローラのメソッド単位にセキュリティ保護する
@AllArgsConstructor // コンストラクタを自動生成する
public class PatientSecurityConfig {

	// DataSource 型のインスタンスをフィールドとする
	private DataSource dataSource;

//	@Bean // アプリケーション実行時にこのメソッドが実行されインスタンスが作られる
//	// セキュリティ保護するページをURL 単位に定義する
//	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//		http.csrf(csrf -> csrf.disable()); // CSRF 対策の無効化
//		http.authorizeHttpRequests(authorize -> {
//			authorize
//            .requestMatchers("/login").permitAll() //ただし、ログイン画面だけは例外とする
//            .anyRequest().authenticated();//urlを直接踏まれてもログインを通すようにする
//		});
//		http.formLogin(form -> { // フォームログインの設定
//			form.defaultSuccessUrl("/show_patient") // ログイン後のデフォルト遷移画面を指定
//					.loginPage("/login"); // ログインページを指定
//		});
//		return http.build();
//	}
	
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http.csrf(csrf -> csrf.disable());
	    http.authorizeHttpRequests(authorize -> {
	        authorize
	            .requestMatchers(         "/login",
	                    "/style.css",
	                    "/css/**",
	                    "/js/**",
	                    "/images/**",
	                    "/favicon.ico").permitAll()
	            .anyRequest().authenticated();
	    });
	    http.formLogin(form -> {
	        form
	            .loginPage("/login")
	            .defaultSuccessUrl("/show_patient",true) //ログイン後は絶対にこのurlに移動する
	            .failureUrl("/login?error=true")
	            .permitAll();
	    });
	    return http.build();
	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}

	@Bean // アプリケーション実行時にこのメソッドが実行されインスタンスが作られる
	// ユーザ管理マネージャを定義する
	UserDetailsManager userDetailsManager() {
		
		/*
		 * このコードの説明。これはハッシュ化したパスワードを手動で登録する場合。
		 * 一時的にパスワード文字列からハッシュ値に変換する
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String hash = encoder.encode("123");
		System.out.println("これがあああああああああああああああああああああ暗号化パスワード！！！！"+hash); //こいつをパスワードとしてsqlに設定する
		*/
		

		//デフォルトでは権限も使う設定になっているため、権限は使いませんという命令を送る
		   JdbcUserDetailsManager manager =
		            new JdbcUserDetailsManager(this.dataSource);
		    manager.setUsersByUsernameQuery(
		        "SELECT username, password, enabled FROM users WHERE username = ?"
		    );
		    manager.setAuthoritiesByUsernameQuery(
		        "SELECT username, 'ROLE_USER' FROM users WHERE username = ?"
		    );

		    return manager;

		// データベースを使ったユーザ管理マネージャを生成
	//	return new JdbcUserDetailsManager(this.dataSource);
	}

}
