package io.hhplus.tdd.point;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;

import java.util.List;

import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	public static final int MAX_POINT = 1_000_000;
	private final UserPointTable userPointRepo;

	private final PointHistoryTable pointHistoryRepo;

	public UserPoint get(long id) {
		return userPointRepo.selectById(id);
	}

	public List<PointHistory> getHistoriesBy(long id) {
		return pointHistoryRepo.selectAllByUserId(id);
	}

	public UserPoint charge(long id, long amount, long chargedAt) {
		var userPoint = userPointRepo.selectById(id);
		if (userPoint.point() + amount > MAX_POINT) {
			throw new IllegalArgumentException("포인트가 최대 잔고를 초과하였습니다");
		}
		var chargedUserPoint = userPointRepo.insertOrUpdate(id, userPoint.point() + amount);
		pointHistoryRepo.insert(id, amount, CHARGE, chargedAt);

		return chargedUserPoint;
	}

	public UserPoint use(long id, long amount, long usedAt) {
		var userPoint = userPointRepo.selectById(id);
		if (userPoint.point() < amount) {
			throw new IllegalArgumentException("포인트가 부족합니다");
		}
		UserPoint usedUserPoint = userPointRepo.insertOrUpdate(id, userPoint.point() - amount);
		pointHistoryRepo.insert(id, amount, USE, usedAt);

		return usedUserPoint;
	}

}
