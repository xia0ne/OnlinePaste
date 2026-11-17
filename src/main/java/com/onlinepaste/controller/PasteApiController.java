package com.onlinepaste.controller;

import com.onlinepaste.domain.Paste;
import com.onlinepaste.service.PasteService;
import com.onlinepaste.web.dto.CreatePasteRequest;
import com.onlinepaste.web.dto.CreatePasteResponse;
import com.onlinepaste.web.dto.PasteResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/pastes")
public class PasteApiController {

	private final PasteService pasteService;

	public PasteApiController(PasteService pasteService) {
		this.pasteService = pasteService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public CreatePasteResponse create(@Valid @RequestBody CreatePasteRequest request) {
		Paste paste = pasteService.create(request);
		return CreatePasteResponse.from(paste, pasteService.shareUrl(paste.id()));
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public PasteResponse get(@PathVariable String id) {
		return PasteResponse.from(pasteService.get(id, true));
	}

	@GetMapping(value = "/{id}/qr.svg", produces = "image/svg+xml")
	public ResponseEntity<String> qrCode(@PathVariable String id) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
				.contentType(MediaType.valueOf("image/svg+xml"))
				.body(pasteService.qrSvg(id));
	}
}
