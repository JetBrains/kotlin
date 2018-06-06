private var bOrCCounter = 0

fun getBorC() = if (bOrCCounter++ % 2 == 0) getB() else getC()