package foo

fun box(): Boolean {

    val intProgression = IntProgression.fromClosedRange(0, 10, 2)
    assertEquals(10, intProgression.last)

    val longProgression = LongProgression.fromClosedRange(0, 420004200042000L, 420004200042000L)
    assertEquals(420004200042000L, longProgression.last)

    val charProgression = CharProgression.fromClosedRange('a', 'z', 2)
    assertEquals('y', charProgression.last)

    // deprecated
    val byteProgression = ByteProgression.fromClosedRange(1, 127, 2)
    assertEquals(127.toByte(), byteProgression.last)

    val shortProgression = ShortProgression.fromClosedRange(1, 32767, 2)
    assertEquals(32767.toShort(), shortProgression.last)

    return true
}
