// EXPECTED_REACHABLE_NODES: 522
package foo

fun box(): String {

    val intProgression = IntProgression.fromClosedRange(0, 10, 2)
    assertEquals(10, intProgression.last)

    val longProgression = LongProgression.fromClosedRange(0, 420004200042000L, 420004200042000L)
    assertEquals(420004200042000L, longProgression.last)

    val charProgression = CharProgression.fromClosedRange('a', 'z', 2)
    assertEquals('y', charProgression.last)

    return "OK"
}
