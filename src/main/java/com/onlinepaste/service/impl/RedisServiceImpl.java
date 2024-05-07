package com.onlinepaste.service.impl;

import com.onlinepaste.domain.DO.InfoDO;
import com.onlinepaste.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {

	@Autowired
	RedisTemplate<String, Object> redisTemplate;

	@Override
	public InfoDO get(String key) {
		Object object = redisTemplate.opsForValue().get(key);
		if(object == null)
			return null;
		InfoDO infoDO = new InfoDO();
		infoDO.setTitle(key);
		infoDO.setContent(object);
		return infoDO;
	}

	@Override
	public Boolean set(InfoDO infoDO) {
		String key = infoDO.getTitle();
		Object value = infoDO.getContent();
		try{
			redisTemplate.opsForValue().set(key, value, 48, TimeUnit.HOURS);
		}catch (Exception e){
			return false;
		}
		return true;
	}

	@Override
	public boolean check(String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}


}
