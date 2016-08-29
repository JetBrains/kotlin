package RoomScanner

fun distance(first: Pair<Double, Double>, second: Pair<Double, Double>): Double {
    val xDistance = first.first - second.first
    val yDistance = first.second - second.second

    return Math.sqrt(xDistance * xDistance + yDistance * yDistance)
}

fun estimateAngle(from: Pair<Double, Double>, to: Pair<Double, Double>): Double =
        Math.atan2(to.first - from.first, to.second - from.second)

fun angleDistance(from: Double, to: Double): Double {
    val min = Math.min(from, to)
    val max = Math.max(from, to)

    val up = max - min
    val down = 360 - max + min

    if (Math.abs(up) < Math.abs(down)) return up else return down
}