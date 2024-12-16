package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.hhplus.tdd.config.ServiceIntegrationTest;

class PointServiceIntegrationTest extends ServiceIntegrationTest {

	@Autowired
	PointService pointService;


	@Test
	@DisplayName("동시에 충전을 한 경우 동시에 충전한 만큼 포인트가 증가하여야한다")
	void concurrencyChargeOnlyTest() throws InterruptedException {
		long userId = USER_ID;
		int threadCount = 10;
		int chargeAmount = 100;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointService.charge(userId, chargeAmount, System.currentTimeMillis());
				} finally {
					doneLatch.countDown();
				}
			});
		}

		doneLatch.await();
		executor.shutdown();

		assertThat(pointService.get(userId).point()).isEqualTo(TEST_INIT_AMOUNT + (threadCount * chargeAmount));
	}

	@Test
	@DisplayName("동시에 사용한 경우 동시에 사용한 만큼 포인트가 사용되어야한다")
	void concurrencyUseOnlyTest() throws InterruptedException {
		long userId = USER_ID;

		int threadCount = 10;
		int useAmount = 50;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointService.use(userId, useAmount, System.currentTimeMillis());
				} finally {
					doneLatch.countDown();
				}
			});
		}
		doneLatch.await();
		executor.shutdown();

		assertThat(pointService.get(userId).point()).isEqualTo(TEST_INIT_AMOUNT - (threadCount * useAmount));
	}

	@Test
	@DisplayName("동시에 충전, 사용한 경우 충전, 사용한 만큼만 포인트가 변경되어야한다")
	void concurrencyChargeAndUseTest() throws InterruptedException {
		long userId = USER_ID;

		int threadCount = 10;
		int chargeAmount = 50;
		int useAmount = 50;

		int chargeThreadCount = 5;
		int useThreadCount = 5;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			final int idx = i;
			executor.submit(() -> {
				try {
					if (idx < chargeThreadCount) {
						pointService.charge(userId, chargeAmount, System.currentTimeMillis());
					} else {
						pointService.use(userId, useAmount, System.currentTimeMillis());
					}
				} finally {
					doneLatch.countDown();
				}
			});
		}
		doneLatch.await();
		executor.shutdown();

		assertThat(pointService.get(userId).point()).isEqualTo(
			TEST_INIT_AMOUNT + (chargeThreadCount * chargeAmount) - (useThreadCount * useAmount));
	}

	@Test
	@DisplayName("동시에 10개의 충전과 1개의 사용이 오는 경우에도 순서대로 처리되어야한다")
	void orderEnforcedTest() throws InterruptedException {

		int chargeThreads = 10;
		int chargeAmount = 1000;
		int useAmount = TEST_INIT_AMOUNT + chargeAmount * chargeThreads;

		ExecutorService executor = Executors.newFixedThreadPool(chargeThreads + 1);
		CountDownLatch doneLatch = new CountDownLatch(chargeThreads + 1);

		for (int i = 0; i < chargeThreads; i++) {
			executor.submit(() -> {
				try {
					pointService.charge(USER_ID, chargeAmount, System.currentTimeMillis());
				} finally {
					doneLatch.countDown();
				}
			});
		}

		executor.submit(() -> {
			try {
				pointService.use(USER_ID, useAmount, System.currentTimeMillis());
			} finally {
				doneLatch.countDown();
			}
		});

		doneLatch.await();
		executor.shutdown();

		assertThat(pointService.get(USER_ID).point()).isEqualTo(0);
	}

}