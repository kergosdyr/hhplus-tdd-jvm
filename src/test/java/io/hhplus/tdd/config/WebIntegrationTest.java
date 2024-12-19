package io.hhplus.tdd.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import io.hhplus.tdd.point.PointService;

@WebMvcTest
public class WebIntegrationTest {

	@Autowired
	public MockMvc mockMvc;

	@MockBean
	public PointService pointService;

}
