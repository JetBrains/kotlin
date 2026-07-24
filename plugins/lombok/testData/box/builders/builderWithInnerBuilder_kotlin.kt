import lombok.Builder
import kotlin.test.assertEquals

@Builder
class Klass(val str: String) {
    @Builder
    class Inner(val integer: Int)
}

fun box(): String {
    val innerBuilder: Klass.Inner.InnerBuilder = Klass.Inner.builder()
    val inner: Klass.Inner = innerBuilder.integer(42).build()

    assertEquals(42, inner.integer)

    val klassBuilder: Klass.KlassBuilder = Klass.builder()
    val klass: Klass = klassBuilder.str("hello").build()

    assertEquals("hello", klass.str)
    return "OK"
}
