package algorithm

import RouteMetricRequest

fun fromIntToDirection(value: Int): String {
    return when (value) {
        0 -> "FORWARD"
        1 -> "BACKWARD"
        2 -> "LEFT"
        3 -> "RIGHT"
        else -> throw IllegalArgumentException("Error parsing Direction from Int: got value = $value")
    }
}

fun RouteMetricRequest.toString(): String {
    var res = emptyArray<String>()

    for (i in distances.indices) {
        val dist = distances[i].toString()
        val dir = fromIntToDirection(directions[i])
        res += "(" + dist + ", " + dir + ")"
    }

    return res.joinToString("; ")
}