package io.hhplus.tdd.point;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

@Component
public class PointLockProvider {

	private final ReentrantLock lock = new ReentrantLock();

	public ReentrantLock provide() {
		return lock;
	}

}
