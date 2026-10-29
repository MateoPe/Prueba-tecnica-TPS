package com.ecommerce.tps.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class DiscountPolicy {
	private final Instant startsAt;
	private final Instant endsAt;
	private final BigDecimal standardPercent;
	private final BigDecimal randomPercent;
	private final BigDecimal frequentPercent;
	private final int frequentOrderThreshold;

	public DiscountPolicy(@Value("${app.discount.starts-at:}") String startsAt,
			@Value("${app.discount.ends-at:}") String endsAt,
			@Value("${app.discount.standard-percent:10}") BigDecimal standardPercent,
			@Value("${app.discount.random-percent:50}") BigDecimal randomPercent,
			@Value("${app.discount.frequent-percent:5}") BigDecimal frequentPercent,
			@Value("${app.discount.frequent-order-threshold:5}") int frequentOrderThreshold) {
		this.startsAt = startsAt.isBlank() ? null : Instant.parse(startsAt);
		this.endsAt = endsAt.isBlank() ? null : Instant.parse(endsAt);
		if ((this.startsAt == null) != (this.endsAt == null)
				|| this.startsAt != null && !this.startsAt.isBefore(this.endsAt)) {
			throw new IllegalArgumentException("Discount start/end must both be set and start before end");
		}
		if (standardPercent.signum() < 0 || randomPercent.signum() < 0 || frequentPercent.signum() < 0
				|| standardPercent.add(frequentPercent).compareTo(new BigDecimal("100")) > 0
				|| randomPercent.add(frequentPercent).compareTo(new BigDecimal("100")) > 0
				|| frequentOrderThreshold < 1) {
			throw new IllegalArgumentException("Discount percentages and frequent-order threshold are invalid");
		}
		this.standardPercent = standardPercent;
		this.randomPercent = randomPercent;
		this.frequentPercent = frequentPercent;
		this.frequentOrderThreshold = frequentOrderThreshold;
	}

	public boolean isWindowActive(Instant now) {
		return startsAt != null && !now.isBefore(startsAt) && now.isBefore(endsAt);
	}

	public BigDecimal percent(boolean randomOrder, boolean frequentCustomer, Instant now) {
		if (!isWindowActive(now)) {
			return BigDecimal.ZERO;
		}
		BigDecimal discount = randomOrder ? randomPercent : standardPercent;
		return frequentCustomer ? discount.add(frequentPercent) : discount;
	}

	public int getFrequentOrderThreshold() { return frequentOrderThreshold; }
}
