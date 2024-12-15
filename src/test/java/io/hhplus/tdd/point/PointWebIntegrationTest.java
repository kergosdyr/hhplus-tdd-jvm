package io.hhplus.tdd.point;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import io.hhplus.tdd.config.WebIntegrationTest;

class PointWebIntegrationTest extends WebIntegrationTest {

	@Test
	@DisplayName("point/{id} 주소로 Get 요청을 보내면 해당하는 User 의 정보를 가져온다")
	void getUser() throws Exception {

		//given
		long userId = 1L;
		long point = 100L;
		long createdAt = System.currentTimeMillis();
		UserPoint mockUserPoint = new UserPoint(userId, point, createdAt);

		//when
		when(pointService.get(userId)).thenReturn(mockUserPoint);

		//then
		mockMvc.perform(get("/point/{id}", userId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(userId))
			.andExpect(jsonPath("$.point").value(point))
			.andExpect(jsonPath("$.updateMillis").value(createdAt));

	}

	@Test
	@DisplayName("point/{id}/histories 주소로 Get 요청을 보내면 해당하는 포인트 히스토리 리스트를 가져온다")
	void getHistories() throws Exception {
		//given
		long userId = 2L;
		long now = System.currentTimeMillis();
		PointHistory h1 = new PointHistory(1L, userId, 50L, TransactionType.CHARGE, now);
		PointHistory h2 = new PointHistory(2L, userId, -20L, TransactionType.USE, now + 1000);

		List<PointHistory> mockHistories = List.of(h1, h2);

		//when
		when(pointService.getHistoriesBy(userId)).thenReturn(mockHistories);

		//then
		mockMvc.perform(get("/point/{id}/histories", userId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(h1.id()))
			.andExpect(jsonPath("$[0].userId").value(h1.userId()))
			.andExpect(jsonPath("$[0].amount").value(h1.amount()))
			.andExpect(jsonPath("$[0].type").value(h1.type().toString()))
			.andExpect(jsonPath("$[0].updateMillis").value(h1.updateMillis()))
			.andExpect(jsonPath("$[1].id").value(h2.id()))
			.andExpect(jsonPath("$[1].userId").value(h2.userId()))
			.andExpect(jsonPath("$[1].amount").value(h2.amount()))
			.andExpect(jsonPath("$[1].type").value(h2.type().toString()))
			.andExpect(jsonPath("$[1].updateMillis").value(h2.updateMillis()));
	}

	@Test
	@DisplayName("point/{id}/charge 주소로 Patch 요청을 보내면 해당하는 포인트를 충전하고 변경된 포인트를 반환한다")
	void chargePoint() throws Exception {
		// given
		long userId = 3L;
		long amount = 200L;
		long updatedAt = System.currentTimeMillis();
		UserPoint updatedUserPoint = new UserPoint(userId, 700L, updatedAt);

		when(pointService.charge(eq(userId), eq(amount), anyLong())).thenReturn(updatedUserPoint);

		// then
		mockMvc.perform(patch("/point/{id}/charge", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(userId))
			.andExpect(jsonPath("$.point").value(700L))
			.andExpect(jsonPath("$.updateMillis").value(updatedAt));
	}

	@Test
	@DisplayName("point/{id}/use 주소로 Patch 요청을 보내면 해당하는 포인트를 사용하고 변경된 포인트를 반환한다")
	void usePoint() throws Exception {
		// given
		long userId = 4L;
		long amount = 50L;
		long updatedAt = System.currentTimeMillis();
		UserPoint updatedUserPoint = new UserPoint(userId, 400L, updatedAt);

		when(pointService.use(eq(userId), eq(amount), anyLong())).thenReturn(updatedUserPoint);

		// then
		mockMvc.perform(patch("/point/{id}/use", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(String.valueOf(amount)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(userId))
			.andExpect(jsonPath("$.point").value(400L))
			.andExpect(jsonPath("$.updateMillis").value(updatedAt));

	}

}