data class First(val x: Array<String>, val y: Int)

class Second(val x: Array<String>)

data class Third(val x: String, val y: IntArray)

data class Correct(val x: IntArray) {
    override fun equals(other: Any?) = other is Correct

    override fun hashCode() = 0
}
