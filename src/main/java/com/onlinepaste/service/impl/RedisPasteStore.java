package com.onlinepaste.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinepaste.domain.Paste;
import com.onlinepaste.service.PasteStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis backed store. Payloads are stored as JSON strings and Redis handles
 * expiration, so an expired paste disappears without a scheduled job.
 */
@Repository
public class RedisPasteStore implements PasteStore {

	private static final Logger log = LoggerFactory.getLogger(RedisPasteStore.class);
	private static final String PASTE_KEY_PREFIX = "onlinepaste:paste:";
	private static final String VIEW_KEY_PREFIX = "onlinepaste:views:";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public RedisPasteStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean saveIfAbsent(Paste paste, Duration ttl) {
		String key = PASTE_KEY_PREFIX + paste.id();
		String json;
		try {
			json = objectMapper.writeValueAsString(paste);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("failed to serialize paste " + paste.id(), e);
		}

		Boolean saved = ttl == null || ttl.isZero() || ttl.isNegative()
				? redisTemplate.opsForValue().setIfAbsent(key, json)
				: redisTemplate.opsForValue().setIfAbsent(key, json, ttl);
		return Boolean.TRUE.equals(saved);
	}

	@Override
	public Optional<Paste> find(String id) {
		String json = redisTemplate.opsForValue().get(PASTE_KEY_PREFIX + id);
		if (json == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, Paste.class));
		} catch (JsonProcessingException e) {
			log.warn("Ignoring unreadable paste payload for id {}", id);
			return Optional.empty();
		}
	}

	@Override
	public boolean exists(String id) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(PASTE_KEY_PREFIX + id));
	}

	@Override
	public long incrementViews(String id, Duration ttl) {
		String key = VIEW_KEY_PREFIX + id;
		Long views = redisTemplate.opsForValue().increment(key);
		if (views == null) {
			return 0;
		}
		if (views == 1L && ttl != null && !ttl.isZero() && !ttl.isNegative()) {
			redisTemplate.expire(key, ttl);
		}
		return views;
	}
}
