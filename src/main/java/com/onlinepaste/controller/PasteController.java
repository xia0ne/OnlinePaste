package com.onlinepaste.controller;

import com.onlinepaste.domain.DO.InfoDO;
import com.onlinepaste.domain.VO.InfoVO;
import com.onlinepaste.service.impl.RedisServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PasteController {
	@Autowired
	RedisServiceImpl redisService;

	@PostMapping("/loadMarkdown")
	public InfoDO getMarkdown(@RequestBody InfoVO infoVO){
		return redisService.get(infoVO.getTitle());
	}

	@PostMapping("/saveMarkdown")
	public void setMarkdown(@RequestBody InfoDO infoDO){
		redisService.set(infoDO);
	}
}
