data class XYZ(val x: Int, val y: Int, val z: Int)

fun test() {
    XYZ(1, 2, 3)./*rename*/copy(x = 10)
}