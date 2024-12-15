package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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
	@DisplayName("ID 를 보내면 해당하는 포인트를 가져온다")
	void getTest() {

		// given

		// when
		UserPoint userPoint = pointService.get(USER_ID);

		// then
		assertThat(userPoint.id()).isEqualTo(USER_ID);
		assertThat(userPoint.point()).isEqualTo(TEST_INIT_AMOUNT);
	}

	@Test
	@DisplayName("포인트를 충전하면 충전한만큼 포인트가 증가한다")
	void chargeSuccessTest() {

		//when
		var chargedUserPoint = pointService.charge(USER_ID, 10000, System.currentTimeMillis());

		//then
		assertThat(chargedUserPoint.point()).isEqualTo(TEST_INIT_AMOUNT + 10000);

	}

	@Test
	@DisplayName("포인트를 충전하면 포인트 히스토리에 충전 기록이 추가된다")
	void historySavedWhenChargeSuccessTest() {

		long chargedAt = System.currentTimeMillis();
		pointService.charge(USER_ID, 10000, chargedAt);
		List<PointHistory> pointHistories = pointHistoryRepo.selectAllByUserId(USER_ID);

		assertThat(pointHistories).filteredOn(
				pointHistory -> pointHistory.userId() == USER_ID && pointHistory.updateMillis() == chargedAt
					&& pointHistory.type().equals(TransactionType.CHARGE) && pointHistory.amount() == 10000)
			.hasSizeGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("최대 충전가능 포인트보다 많이 충전하려고 하면 익셉션이 발생한다")
	void chargePointOverMaxThrowExceptionTest() {

		assertThatThrownBy(() -> {
			pointService.charge(USER_ID, TEST_INIT_AMOUNT + 1_000_000, System.currentTimeMillis());
		}).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("포인트가 최대 잔고를 초과하였습니다");

	}

	@Test
	@DisplayName("포인트를 사용하면 사용한만큼 포인트가 감소한다")
	void useSuccessTest() {

		//when
		var usedUserPoint = pointService.use(USER_ID, 5000, System.currentTimeMillis());

		//then
		assertThat(usedUserPoint.point()).isEqualTo(TEST_INIT_AMOUNT - 5000);

	}

	@Test
	@DisplayName("현재 가지고 있는 포인트보다 많이 사용하려고 하면 익셉션이 발생한다")
	void usePointOverOwnedThrowExceptionTest() {

		assertThatThrownBy(() -> {
			pointService.use(USER_ID, TEST_INIT_AMOUNT + 1, System.currentTimeMillis());
		}).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("포인트가 부족합니다");

	}

	@Test
	@DisplayName("포인트를 사용하면 포인트 히스토리에 사용 기록이 추가된다")
	void historySavedWhenUseSuccessTest() {

		long usedAt = System.currentTimeMillis();
		pointService.use(USER_ID, 5000, System.currentTimeMillis());
		List<PointHistory> pointHistories = pointHistoryRepo.selectAllByUserId(USER_ID);

		assertThat(pointHistories).filteredOn(
				pointHistory -> pointHistory.userId() == USER_ID && pointHistory.updateMillis() == usedAt
					&& pointHistory.type().equals(TransactionType.USE) && pointHistory.amount() == 5000)
			.hasSizeGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("10개의 스레드로 동시에 충전을 한 경우 10번에 해당하는 포인트가 증가하여야한다")
	void concurrencyChargeOnlyTest() throws InterruptedException {
		long userId = USER_ID;
		int threadCount = 10;
		int chargeAmount = 100; // 한번 충전 시 100 증가

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);

		// 스레드 시작. 여기서는 동시에 시작을 강제하지 않고 그냥 바로 실행
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

		long expected = TEST_INIT_AMOUNT + (threadCount * chargeAmount);
		long actual = pointService.get(userId).point();

		assertThat(actual)
			.as("charge 전용 동시 작업 후 포인트가 예상치와 다릅니다.")
			.isEqualTo(expected);
	}

	@Test
	@DisplayName("10개의 스레드로 동시에 사용한 경우 10번에 해당하는 포인트가 사용되어야한다")
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

		long expectedFinal = TEST_INIT_AMOUNT - (threadCount * useAmount);
		long actualFinal = pointService.get(userId).point();

		assertThat(actualFinal)
			.as("use 전용 동시 작업 후 포인트가 예상치와 다릅니다.")
			.isEqualTo(expectedFinal);
	}

	@Test
	@DisplayName("10개의 스레드 중 5개는 충전, 5개는 사용하는 경우 원래의 값과 같아야한다")
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

		long expected = TEST_INIT_AMOUNT + (chargeThreadCount * chargeAmount) - (useThreadCount * useAmount);
		long actual = pointService.get(userId).point();

		assertThat(actual)
			.as("charge/use 혼합 동시 작업 후 포인트가 예상치와 다릅니다.")
			.isEqualTo(expected);
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