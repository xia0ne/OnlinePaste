package com.onlinepaste.support;

import com.onlinepaste.domain.Paste;
import com.onlinepaste.service.PasteStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory store used by tests so the suite never needs a running Redis.
 */
public class InMemoryPasteStore implements PasteStore {

	private final Map<String, Paste> pastes = new ConcurrentHashMap<>();
	private final Map<String, AtomicLong> views = new ConcurrentHashMap<>();

	@Override
	public boolean saveIfAbsent(Paste paste, Duration ttl) {
		return pastes.putIfAbsent(paste.id(), paste) == null;
	}

	@Override
	public Optional<Paste> find(String id) {
		Paste paste = pastes.get(id);
		if (paste == null) {
			return Optional.empty();
		}
		if (paste.expiresAt() != null && !paste.expiresAt().isAfter(Instant.now())) {
			pastes.remove(id);
			return Optional.empty();
		}
		return Optional.of(paste);
	}

	@Override
	public boolean exists(String id) {
		return find(id).isPresent();
	}

	@Override
	public long incrementViews(String id, Duration ttl) {
		return views.computeIfAbsent(id, key -> new AtomicLong()).incrementAndGet();
	}

	public void clear() {
		pastes.clear();
		views.clear();
	}
}
