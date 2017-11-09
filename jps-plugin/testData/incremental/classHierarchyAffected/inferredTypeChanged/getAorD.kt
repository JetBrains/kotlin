private var aOrDCounter = 0

fun getAorD() = if (aOrDCounter++ % 2 == 0) getA() else getD()