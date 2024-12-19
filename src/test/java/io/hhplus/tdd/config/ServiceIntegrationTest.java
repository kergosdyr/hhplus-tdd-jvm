package io.hhplus.tdd.config;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@SpringBootTest
public class ServiceIntegrationTest {

	public static final int TEST_INIT_AMOUNT = 10000;

	public static final int USER_ID = 1;

	@Autowired
	public UserPointTable userPointRepo;

	@Autowired
	public PointHistoryTable pointHistoryRepo;

	@BeforeEach
	void setUp() {
		userPointRepo.insertOrUpdate(USER_ID, TEST_INIT_AMOUNT);
	}

}
