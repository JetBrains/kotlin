package lib

value class Point private constructor(val first: Int, val second: Int) {
    companion object {
        fun of(x: Int) = Point(x, x + 1)
    }
}

value object Empty
