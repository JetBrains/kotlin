package foo

fun box(): Boolean {
    val a = 2
    if (!(a equals a)) return false
    if (!(a equals 2)) return false
    if (!(a equals 2.0)) return false
    val c = "a"
    if (!("a" equals c)) return false
    if (!((null : Any?)?.equals(null) ?: true)) return false
    val d = 5.6
    if (!(d.toShort() equals 5.toShort())) return false
    if (!(d.toByte() equals 5.toByte())) return false
    if (!(d.toFloat() equals 5.6.toFloat())) return false
    if (!(d.toInt() equals 5)) return false
    if (true equals false) return false

    val n: Number = 3
    if (!(n equals 3.3.toInt())) return false
    return true
}