import kotlin.test.*

object RGBA {
    fun packFast(r: Int, g: Int, b: Int, a: Int) = (r shl 0) or (g shl 8) or (b shl 16) or (a shl 24)

    fun getFastR(v: Int): Int = (v ushr 0) and 0xFF
    fun getFastG(v: Int): Int = (v ushr 8) and 0xFF
    fun getFastB(v: Int): Int = (v ushr 16) and 0xFF
    fun getFastA(v: Int): Int = (v ushr 24) and 0xFF

    fun premultiplyFastInt(v: Int): Int {
        val A = getFastA(v) + 1
        val RB = (((v and 0x00FF00FF) * A) ushr 8) and 0x00FF00FF
        val G = (((v and 0x0000FF00) * A) ushr 8) and 0x0000FF00
        return (v and 0x00FFFFFF.inv()) or RB or G
    }
}

fun box(): String {
    val source = listOf(0xFFFFFFFF.toInt(), 0xFFFFFF77.toInt(), 0x777777FF.toInt(), 0x77777777.toInt())
    val expect = listOf(-1, -137, 2000107383, 2000107319)
    assertEquals(expect, source.map { RGBA.premultiplyFastInt(it) })

    return "OK"
}