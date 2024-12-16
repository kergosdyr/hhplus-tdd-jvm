package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

class PointServiceUnitTest {

	private static final long USER_ID = 1L;
	private static final int TEST_INIT_AMOUNT = 10000;

	UserPointTable userPointRepo = new UserPointTable();

	PointHistoryTable pointHistoryRepo = new PointHistoryTable();

	PointService pointService = new PointService(userPointRepo, pointHistoryRepo);

	@BeforeEach
	void setUp() {
		// given
		userPointRepo.insertOrUpdate(1L, TEST_INIT_AMOUNT);
	}

	@Test
	@DisplayName("ID 를 보내면 해당하는 포인트를 가져온다")
	void getTest() {

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