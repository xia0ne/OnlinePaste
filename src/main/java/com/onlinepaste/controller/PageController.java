package com.onlinepaste.controller;

import com.onlinepaste.config.OnlinePasteProperties;
import com.onlinepaste.domain.Paste;
import com.onlinepaste.domain.PasteWithViews;
import com.onlinepaste.service.PasteService;
import com.onlinepaste.service.exception.PasteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Controller
public class PageController {

	private static final DateTimeFormatter DISPLAY_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

	private final PasteService pasteService;
	private final OnlinePasteProperties properties;

	public PageController(PasteService pasteService, OnlinePasteProperties properties) {
		this.pasteService = pasteService;
		this.properties = properties;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("maxContentLength", properties.maxContentLength());
		return "index";
	}

	@GetMapping("/help")
	public String help(Model model) {
		model.addAttribute("maxContentLength", properties.maxContentLength());
		return "help";
	}

	@GetMapping("/p/{id}")
	public ModelAndView view(@PathVariable String id) {
		PasteWithViews result;
		try {
			result = pasteService.get(id, true);
		} catch (PasteNotFoundException e) {
			ModelAndView notFound = new ModelAndView("not-found", HttpStatus.NOT_FOUND);
			notFound.addObject("pasteId", id);
			return notFound;
		}

		Paste paste = result.paste();
		ModelAndView modelAndView = new ModelAndView("view");
		modelAndView.addObject("pasteId", paste.id());
		modelAndView.addObject("content", paste.content());
		modelAndView.addObject("createdAtText", DISPLAY_FORMAT.format(paste.createdAt()));
		modelAndView.addObject("expiresAtIso", paste.expiresAt() == null ? null : paste.expiresAt().toString());
		modelAndView.addObject("expiresAtText", paste.expiresAt() == null
				? "Never" : DISPLAY_FORMAT.format(paste.expiresAt()));
		modelAndView.addObject("permanent", paste.permanent());
		modelAndView.addObject("views", result.views());
		modelAndView.addObject("shareUrl", pasteService.shareUrl(paste.id()));
		return modelAndView;
	}
}
