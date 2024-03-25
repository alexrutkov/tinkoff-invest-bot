package tinkoff.tinkoffbotapi.extensions

import ru.tinkoff.piapi.contract.v1.Quotation
import java.math.BigDecimal


fun Quotation.toDemical(): BigDecimal {
	return if (units == 0L && nano == 0) BigDecimal.ZERO
	else BigDecimal.valueOf(units).add(BigDecimal.valueOf(nano.toLong(), 9))
}

fun BigDecimal.toQuotation(): Quotation {
	return Quotation.newBuilder().setUnits(longValueExact())
		.setNano(remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(1000000000)).intValueExact())
		.build()
}
