package com.onlinepaste.service;

import com.onlinepaste.config.OnlinePasteProperties;
import com.onlinepaste.domain.Paste;
import com.onlinepaste.domain.PasteWithViews;
import com.onlinepaste.service.exception.PasteConflictException;
import com.onlinepaste.service.exception.PasteNotFoundException;
import com.onlinepaste.service.exception.PasteValidationException;
import com.onlinepaste.service.impl.PasteServiceImpl;
import com.onlinepaste.support.InMemoryPasteStore;
import com.onlinepaste.web.dto.CreatePasteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasteServiceImplTest {

	private InMemoryPasteStore store;
	private PasteServiceImpl service;
	private OnlinePasteProperties properties;

	@BeforeEach
	void setUp() {
		properties = new OnlinePasteProperties(
				"https://paste.example/", 100_000, 10, true,
				new OnlinePasteProperties.RateLimit(true, 30));
		store = new InMemoryPasteStore();
		service = new PasteServiceImpl(store, new QrCodeService(), properties);
	}

	@Test
	void createsPasteWithOneDayExpiryByDefault() {
		Paste paste = service.create(new CreatePasteRequest("hello", null, null));

		assertThat(paste.id()).hasSize(10);
		assertThat(paste.createdAt()).isNotNull();
		assertThat(paste.expiresAt()).isAfter(Instant.now().plus(Duration.ofHours(23)));
		assertThat(paste.expiresAt()).isBefore(Instant.now().plus(Duration.ofHours(25)));
	}

	@Test
	void createsPermanentPaste() {
		Paste paste = service.create(new CreatePasteRequest("forever", "never", null));

		assertThat(paste.permanent()).isTrue();
		assertThat(paste.expiresAt()).isNull();
	}

	@Test
	void createsPasteWithOneHourExpiry() {
		Paste paste = service.create(new CreatePasteRequest("short", "1h", null));

		assertThat(paste.expiresAt()).isBetween(
				Instant.now().plus(Duration.ofMinutes(55)),
				Instant.now().plus(Duration.ofMinutes(65)));
	}

	@Test
	void acceptsCustomId() {
		Paste paste = service.create(new CreatePasteRequest("hello", "7d", "my-note"));

		assertThat(paste.id()).isEqualTo("my-note");
		assertThat(service.get("my-note", false).paste().content()).isEqualTo("hello");
	}

	@Test
	void rejectsReservedCustomId() {
		assertThatThrownBy(() -> service.create(new CreatePasteRequest("hello", "1d", "help")))
				.isInstanceOf(PasteValidationException.class)
				.hasMessageContaining("reserved");
	}

	@Test
	void rejectsInvalidCustomId() {
		assertThatThrownBy(() -> service.create(new CreatePasteRequest("hello", "1d", "a b")))
				.isInstanceOf(PasteValidationException.class);
	}

	@Test
	void rejectsDuplicateCustomId() {
		service.create(new CreatePasteRequest("first", "1d", "same-id"));

		assertThatThrownBy(() -> service.create(new CreatePasteRequest("second", "1d", "same-id")))
				.isInstanceOf(PasteConflictException.class);
	}

	@Test
	void rejectsUnknownExpiration() {
		assertThatThrownBy(() -> service.create(new CreatePasteRequest("hello", "2w", null)))
				.isInstanceOf(PasteValidationException.class)
				.hasMessageContaining("expiration");
	}

	@Test
	void rejectsBlankContent() {
		assertThatThrownBy(() -> service.create(new CreatePasteRequest("   ", "1d", null)))
				.isInstanceOf(PasteValidationException.class);
	}

	@Test
	void enforcesConfiguredContentLimit() {
		properties = new OnlinePasteProperties(
				"https://paste.example", 5, 10, true,
				new OnlinePasteProperties.RateLimit(true, 30));
		service = new PasteServiceImpl(store, new QrCodeService(), properties);

		assertThatThrownBy(() -> service.create(new CreatePasteRequest("123456", "1d", null)))
				.isInstanceOf(PasteValidationException.class)
				.hasMessageContaining("5");
	}

	@Test
	void countsViewsOnlyWhenRequested() {
		Paste paste = service.create(new CreatePasteRequest("hello", "1d", null));

		assertThat(service.get(paste.id(), false).views()).isZero();
		assertThat(service.get(paste.id(), true).views()).isEqualTo(1);
		assertThat(service.get(paste.id(), true).views()).isEqualTo(2);
	}

	@Test
	void throwsForUnknownId() {
		assertThatThrownBy(() -> service.get("missing-id", true))
				.isInstanceOf(PasteNotFoundException.class);
	}

	@Test
	void throwsForMalformedIdWithoutHittingTheStore() {
		assertThatThrownBy(() -> service.get("../../etc/passwd", true))
				.isInstanceOf(PasteNotFoundException.class);
	}

	@Test
	void shareUrlHasNoDoubleSlash() {
		assertThat(service.shareUrl("abc123")).isEqualTo("https://paste.example/p/abc123");
	}

	@Test
	void qrCodeIsSvg() {
		Paste paste = service.create(new CreatePasteRequest("hello", "1d", null));

		String svg = service.qrSvg(paste.id());

		assertThat(svg).startsWith("<?xml");
		assertThat(svg).contains("<svg");
		assertThat(svg).contains("</svg>");
	}

	@Test
	void viewCounterIsSkippedWhenDisabled() {
		OnlinePasteProperties disabled = new OnlinePasteProperties(
				"https://paste.example", 100_000, 10, false,
				new OnlinePasteProperties.RateLimit(true, 30));
		service = new PasteServiceImpl(store, new QrCodeService(), disabled);
		Paste paste = service.create(new CreatePasteRequest("hello", "1d", null));

		PasteWithViews result = service.get(paste.id(), true);

		assertThat(result.views()).isZero();
	}
}
