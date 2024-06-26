package com.onlinepaste.controller;

import lombok.extern.java.Log;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@Log
public class IndexController {
	@GetMapping("/")
	public String hello(){
		return "help";
	}
	@GetMapping("/{title}")
	public String handleTitle(@PathVariable String title, Model model){

		model.addAttribute("title", title);
		return "index";
	}
}
