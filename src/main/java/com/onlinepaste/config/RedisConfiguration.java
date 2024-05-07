package com.onlinepaste.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {
	@Bean
	public RedisSerializer<String> redisKeySerializer(){
		return new StringRedisSerializer();
	}

	@Bean
	public RedisSerializer<Object> redisValueSerializer(){
		return new GenericJackson2JsonRedisSerializer();
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, RedisSerializer<String> redisKey, RedisSerializer<Object> redisValue){
		RedisTemplate<String, Object> redisTemplate= new RedisTemplate<>();
		redisTemplate.setConnectionFactory(factory);

		redisTemplate.setKeySerializer(redisKey);
		redisTemplate.setHashKeySerializer(redisKey);

		redisTemplate.setValueSerializer(redisValue);
		redisTemplate.setHashValueSerializer(redisValue);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
