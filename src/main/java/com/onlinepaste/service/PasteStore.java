package com.onlinepaste.service;

import com.onlinepaste.domain.Paste;

import java.time.Duration;
import java.util.Optional;

/**
 * Persistence boundary for pastes. The Redis implementation is the only
 * production implementation, tests use an in-memory variant.
 */
public interface PasteStore {

	/**
	 * Stores a paste only when its id is still free.
	 *
	 * @param ttl time to live, or {@code null} for a permanent paste
	 * @return {@code true} when the paste was written
	 */
	boolean saveIfAbsent(Paste paste, Duration ttl);

	Optional<Paste> find(String id);

	boolean exists(String id);

	long incrementViews(String id, Duration ttl);
}
