package com.ecommerce.tps.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscountPolicyTest {
	private final DiscountPolicy policy = new DiscountPolicy(
			"2026-01-01T00:00:00Z", "2027-01-01T00:00:00Z",
			new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("5"), 5);

	@Test
	void standardOrderGetsTenPercentInsideWindow() {
		assertEquals(new BigDecimal("10"), policy.percent(false, false, Instant.parse("2026-06-01T00:00:00Z")));
	}

	@Test
	void randomOrderReceivesFiftyPercentAndFrequentCustomerAddsFive() {
		assertEquals(new BigDecimal("55"), policy.percent(true, true, Instant.parse("2026-06-01T00:00:00Z")));
	}

	@Test
	void discountsDoNotApplyOutsideConfiguredWindow() {
		assertEquals(BigDecimal.ZERO, policy.percent(true, true, Instant.parse("2027-01-01T00:00:00Z")));
	}
}
