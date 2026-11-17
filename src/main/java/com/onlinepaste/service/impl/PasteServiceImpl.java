package com.onlinepaste.service.impl;

import com.onlinepaste.config.OnlinePasteProperties;
import com.onlinepaste.domain.Expiration;
import com.onlinepaste.domain.Paste;
import com.onlinepaste.domain.PasteWithViews;
import com.onlinepaste.service.PasteService;
import com.onlinepaste.service.PasteStore;
import com.onlinepaste.service.QrCodeService;
import com.onlinepaste.service.exception.PasteConflictException;
import com.onlinepaste.service.exception.PasteNotFoundException;
import com.onlinepaste.service.exception.PasteValidationException;
import com.onlinepaste.web.dto.CreatePasteRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PasteServiceImpl implements PasteService {

	private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{4,64}$");
	private static final String ID_ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final int MAX_ID_ATTEMPTS = 5;
	private static final Set<String> RESERVED_IDS = Set.of(
			"api", "p", "help", "assets", "actuator", "error", "index", "new", "robots.txt");

	private final PasteStore pasteStore;
	private final QrCodeService qrCodeService;
	private final OnlinePasteProperties properties;
	private final SecureRandom random = new SecureRandom();

	public PasteServiceImpl(PasteStore pasteStore, QrCodeService qrCodeService, OnlinePasteProperties properties) {
		this.pasteStore = pasteStore;
		this.qrCodeService = qrCodeService;
		this.properties = properties;
	}

	@Override
	public Paste create(CreatePasteRequest request) {
		String content = request.content();
		if (!StringUtils.hasText(content)) {
			throw new PasteValidationException("content must not be blank");
		}
		if (content.length() > properties.maxContentLength()) {
			throw new PasteValidationException(
					"content must not exceed " + properties.maxContentLength() + " characters");
		}

		Expiration expiration = Expiration.fromCode(request.expiration())
				.orElseThrow(() -> new PasteValidationException("expiration must be one of 1h, 1d, 7d, never"));

		Instant createdAt = Instant.now();
		Paste paste;
		if (StringUtils.hasText(request.id())) {
			String customId = request.id().trim();
			validateId(customId);
			paste = new Paste(customId, content, createdAt, expiration.expiresAt(createdAt));
			if (!pasteStore.saveIfAbsent(paste, expiration.getDuration())) {
				throw new PasteConflictException("id is already in use: " + customId);
			}
			return paste;
		}

		Duration ttl = expiration.getDuration();
		for (int attempt = 0; attempt < MAX_ID_ATTEMPTS; attempt++) {
			paste = new Paste(generateId(), content, createdAt, expiration.expiresAt(createdAt));
			if (pasteStore.saveIfAbsent(paste, ttl)) {
				return paste;
			}
		}
		throw new IllegalStateException("could not generate a unique paste id");
	}

	@Override
	public PasteWithViews get(String id, boolean countView) {
		if (id == null || !ID_PATTERN.matcher(id).matches()) {
			throw new PasteNotFoundException(String.valueOf(id));
		}
		Paste paste = pasteStore.find(id)
				.orElseThrow(() -> new PasteNotFoundException(id));
		long views = 0;
		if (countView && properties.viewCountEnabled()) {
			views = pasteStore.incrementViews(id, paste.remainingTtl(Instant.now()));
		}
		return new PasteWithViews(paste, views);
	}

	@Override
	public String shareUrl(String id) {
		String baseUrl = properties.baseUrl() == null ? "" : properties.baseUrl().trim();
		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl + "/p/" + id;
	}

	@Override
	public String qrSvg(String id) {
		get(id, false);
		return qrCodeService.toSvg(shareUrl(id));
	}

	private void validateId(String id) {
		if (!ID_PATTERN.matcher(id).matches()) {
			throw new PasteValidationException("id must be 4-64 characters of letters, digits, - or _");
		}
		if (RESERVED_IDS.contains(id.toLowerCase())) {
			throw new PasteValidationException("id is reserved: " + id);
		}
	}

	private String generateId() {
		int length = Math.max(6, Math.min(64, properties.idLength()));
		StringBuilder id = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			id.append(ID_ALPHABET.charAt(random.nextInt(ID_ALPHABET.length())));
		}
		return id.toString();
	}
}
