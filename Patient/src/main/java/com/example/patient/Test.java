package com.example.patient;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class Test {
	
	@RequestMapping("/")
	public String start() {
		return "form.html";
	}

	
	@RequestMapping("/register")
	public ModelAndView register(@ModelAttribute RegisterBean rb,
			ModelAndView mv){
		mv.addObject("rb",rb);
		mv.setViewName("register.html");
		return mv;
		
	}

}
