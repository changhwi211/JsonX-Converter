package com.example.demo.cache;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class StatusRepositoryImpl implements StatusRepository{
	
	@Override
	@Cacheable("status")
	public Status getByName(String name) {
		//simulateSlowService();
		return new Status(name);
	}
	
	private void simulateSlowService() {
		try {
			long time = 3000L;
			Thread.sleep(time);
		}catch(InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
}
