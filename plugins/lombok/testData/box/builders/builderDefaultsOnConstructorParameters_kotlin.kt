// ISSUE: KT-90590
import lombok.Builder
import kotlin.test.assertEquals

var counter = 0

fun next(): Int = ++counter

@Builder
class Entity(
    @Builder.Default val a: Int = 1,
    // Not evaluatable at compile time: refers to an earlier parameter
    @Builder.Default val b: Int = a + 1,
    // Not evaluatable at compile time: calls a function
    @Builder.Default val c: Int = next(),
    @Builder.Default val d: Int = X + Y,
) {
    companion object {
        const val X = 10
    }
}

const val Y = 20

fun box(): String {
    val defaults = Entity.builder().build()
    assertEquals(1, defaults.a)
    assertEquals(2, defaults.b)
    assertEquals(1, defaults.c)
    assertEquals(30, defaults.d)

    val custom = Entity.builder().a(5).build()
    assertEquals(5, custom.a)
    assertEquals(6, custom.b)
    assertEquals(2, custom.c)

    val explicit = Entity.builder().a(5).b(0).c(0).d(0).build()
    assertEquals(0, explicit.b)
    assertEquals(0, explicit.c)
    assertEquals(0, explicit.d)
    assertEquals(2, counter)

    return "OK"
}
