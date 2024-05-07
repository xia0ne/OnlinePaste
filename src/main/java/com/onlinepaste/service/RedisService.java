package com.onlinepaste.service;

import com.onlinepaste.domain.DO.InfoDO;

public interface RedisService {
	public InfoDO get(String key);

	public void set(InfoDO infoDO, int hours);

	public boolean check(String key);
}
