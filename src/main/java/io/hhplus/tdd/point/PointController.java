package io.hhplus.tdd.point;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/point")
@Slf4j
@RequiredArgsConstructor
public class PointController {

	private final PointService pointService;

	/**
	 * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
	 */
	@GetMapping("{id}")
	public UserPoint point(@PathVariable long id) {
		return pointService.get(id);
	}

	/**
	 * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
	 */
	@GetMapping("{id}/histories")
	public List<PointHistory> history(@PathVariable long id) {

		return pointService.getHistoriesBy(id);
	}

	/**
	 * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
	 */
	@PatchMapping("{id}/charge")
	public UserPoint charge(@PathVariable long id, @RequestBody long amount) {

		return pointService.charge(id, amount, System.currentTimeMillis());
	}

	/**
	 * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
	 */
	@PatchMapping("{id}/use")
	public UserPoint use(@PathVariable long id, @RequestBody long amount) {
		return pointService.use(id, amount, System.currentTimeMillis());
	}
}
