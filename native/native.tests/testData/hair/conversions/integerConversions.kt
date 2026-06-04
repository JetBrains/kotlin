fun widen(x: Int): Long = x.toLong()
fun narrow(x: Long): Int = x.toInt()
fun truncateToByte(x: Int): Byte = x.toByte()
fun charToInt(c: Char): Int = c.code
fun main() {
    var ri: Int
    var rl: Long
    var rb: Byte

    rl = widen(123);           if (rl != 123L)      error("widen(123) = $rl, expected 123L")
    ri = narrow(9000000000L);  if (ri != 410065408) error("narrow(9000000000L) = $ri, expected 410065408")
    rb = truncateToByte(258);  if (rb.toInt() != 2) error("truncateToByte(258).toInt() = ${rb.toInt()}, expected 2")
    ri = charToInt('A');       if (ri != 65)        error("charToInt('A') = $ri, expected 65")
}
