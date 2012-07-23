package foo

fun box(): Any? {
    if (3 / 4 != 0) {
        return "1"
    }
    if (-5 / 4 != -1) {
        return -5 / 4
    }
    if ((3.0 / 4.0 - 0.75) > 0.01) {
        return "3"
    }
    if ((-10.0 / 4.0 + 2.5) > 0.01) {
        return "4"
    }
    val i1: Int = 5
    val i2: Int = 2
    if (i1 / i2 != 2) {
        return "5"
    }
    val i3: Short = 5
    val i4: Short = 2
    if (i3 / i4 != 2) {
        return "6"
    }
    val i5: Byte = 5
    val i6: Byte = 2
    if (i5 / i6 != 2) {
        return "7"
    }

    val f1: Double = 5.0
    val f2: Double = 2.0
    if ((f1 / f2 - 2.5) > 0.01) {
        return "8"
    }

    val f3: Float = 5.0.toFloat()
    val f4: Float = 2.0.toFloat()
    if ((f3 / f4 - 2.5.toFloat()) > 0.01) {
        return "9"
    }
    return "SUCCESS"
}