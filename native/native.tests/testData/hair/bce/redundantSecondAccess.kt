fun twice(a: IntArray, i: Int): Int = a[i] + a[i]

fun main() {
    val a = IntArray(3)
    a[0] = 3
    a[1] = 5
    a[2] = 7
    val r = twice(a, 1)
    if (r != 10) error("twice(a, 1) = $r, expected 10")

    try {
        twice(a, 3)
        error("twice(a, 3) should throw")
    } catch (e: IndexOutOfBoundsException) {
    }
}
