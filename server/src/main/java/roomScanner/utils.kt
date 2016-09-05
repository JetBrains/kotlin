package roomScanner

import CodedOutputStream

fun horizon(): IntArray {
    val horizon = IntArray(180 / 5, { it * 5 })
    horizon.reverse()

    return horizon
}

fun distance(first: Pair<Double, Double>, second: Pair<Double, Double>): Double {
    val xDistance = first.first - second.first
    val yDistance = first.second - second.second

    return Math.sqrt(xDistance * xDistance + yDistance * yDistance)
}

fun angleDistance(from: Double, to: Double): Double {
    val distance = Math.min(Math.abs(from - to), Math.abs(360 - (from - to)))
    val direction = if (distance == Math.abs(from - to)) Math.signum(to - from) else -Math.signum(to - from)

    return distance * direction
}

inline fun serialize(size: Int, writeTo: (CodedOutputStream) -> Unit): ByteArray {
    val bytes = ByteArray(size)
    writeTo(CodedOutputStream(bytes))

    return bytes
}

fun <T> maxSuffix(first: List<T>, second: List<T>, distance: (T, T) -> Double): Int {
    val distances = first.mapIndexed { i: Int, t: T ->
        var score = 0.0
        var size = Math.min(second.size - 1, first.size - i - 1)
        for (j in 0..size) {
            score += distance(first[i + j], second[j])
        }

        score / (size + 1)
    }.toList()

    return distances.indexOf(distances.min())
}

fun <T> merge(lists: List<List<T>>, distance: (T, T) -> Double): List<T> {
    val indexes = mutableListOf<Int>()
    for (i in 0..(lists.size - 2)) {
        indexes.add(maxSuffix(lists[i], lists[i + 1], distance))
    }

    val result = mutableListOf<T>()
    for (i in 0..(indexes.size - 1)) {
        for (j in 0..indexes[i]) {
            result.add(lists[i][j])
        }
    }

    for (i in (indexes.last() + 1)..(lists.last().size - 1)) {
        result.add(lists.last()[i])
    }

    return result
}
