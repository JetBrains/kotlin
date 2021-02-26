import platform.Accelerate.*

// Custom print
fun Vector128.toStringHex(): String {
    return "0x" + (0 until 16).map { getUByteAt(it).toString(16) }.joinToString("")
}

// Custom print
fun Vector128.toStringFloat(): String {
    return "(${(0 until 4).map { getFloatAt(it).toString() }.joinToString(", ")})"
}


fun main() {

    // Accessors
    val vf4 = vectorOf(1f, 3.162f, 10f, 31f)
    println(vf4)
    println(vf4.toStringFloat())
    println(vf4.toStringHex())
    println(vf4.getFloatAt(1))
    println(vf4.getIntAt(0))
    println(vf4.getByteAt(3))
    // Illegal access (out of bounds)
    try {
        println(vf4.getIntAt(4))
        println("FAILED")
    } catch (e: IndexOutOfBoundsException) {
        println("Handling $e")
    }

    // Assignment and equality
    var x1 = vectorOf(-1f, 0f, 0f, -7f)
    val y1 = vectorOf(-1f, 0f, 0f, -7f)
    var x2 = vectorOf(1f, 4f, 3f, 7f)
    println("(x1 == y1) is ${(x1 == y1)}")
    println("(x1.equals(y1)) is ${(x1.equals(y1))}")
    println("(x1 == x2) is ${(x1 == x2)}")
    x1 = x2
    println("Now (x1 == x1) is ${(x1 == x1)}")


    // Using library function (MacOS Accelerate framework)
    val sum = vS128Add(vectorOf(1,2,3,4), vectorOf(4,3,2,1))
    println(sum)
    // More Accelerate framework
    val q = vectorOf(1f, 9f, 25f, 49f)
    val sq = vsqrtf(q)
    println("vsqrtf$q = ${sq.toStringFloat()}")
    val f4 = vectorOf(1f, 3.162f, 10f, 31f)
    println("vlog10f($f4) = ${vlog10f(vf4).toStringFloat()}")


}
