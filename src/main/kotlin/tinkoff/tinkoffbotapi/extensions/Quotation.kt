package tinkoff.tinkoffbotapi.extensions

import ru.tinkoff.piapi.contract.v1.MoneyValue
import ru.tinkoff.piapi.contract.v1.Quotation
import java.math.BigDecimal


fun Quotation.toDecimal(): BigDecimal {
	return if (units == 0L && nano == 0) BigDecimal.ZERO
	else BigDecimal.valueOf(units).add(BigDecimal.valueOf(nano.toLong(), 9))
}

fun MoneyValue.toDecimal(): BigDecimal {
	return if (units == 0L && nano == 0) BigDecimal.ZERO
	else BigDecimal.valueOf(units).add(BigDecimal.valueOf(nano.toLong(), 9))
}

fun BigDecimal.toQuotation(): Quotation {
	return Quotation.newBuilder().setUnits(toLong())
		.setNano(remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1_000_000_000)).toInt())
		.build()
}
