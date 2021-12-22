private var bOrDCounter = 0

fun getBorD() = if (bOrDCounter++ % 2 == 0) getB() else getD()