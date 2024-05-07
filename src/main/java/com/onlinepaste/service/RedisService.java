package com.onlinepaste.service;

import com.onlinepaste.domain.DO.InfoDO;

public interface RedisService {
	public InfoDO get(String key);

	public Boolean set(InfoDO infoDO);

	public boolean check(String key);
}
