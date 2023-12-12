import kotlin.test.*

fun box(): String {
    assertEquals(1, 0f.compareTo(-0f))
    assertEquals(1, 0.0.compareTo(-0.0))

    assertEquals(1.0, Double.fromBits(1.0.toBits()))
    assertEquals(1.0f, Float.fromBits(1.0f.toBits()))

    assertEquals(Double.NaN, Double.fromBits((0 / 0.0).toBits()))
    assertEquals(Float.NaN, Float.fromBits((0 / 0f).toBits()))

    return "OK"
}
