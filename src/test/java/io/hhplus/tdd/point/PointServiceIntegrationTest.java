package io.hhplus.tdd.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

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

}