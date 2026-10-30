import lombok.Builder
import kotlin.test.assertEquals

class Plain @Builder constructor(val id: Int, var name: String, raw: Int) {
    var body: Int = raw * 2
}

class WithToBuilder @Builder(toBuilder = true) constructor(val id: Int, var name: String)

// The class-level and the constructor builder build out of the same parameters, so they share one builder class
@Builder
class BothAnnotations @Builder constructor(val id: Int)

fun box(): String {
    val plain = Plain.builder().id(1).name("a").raw(3).build()
    assertEquals(1, plain.id)
    assertEquals("a", plain.name)
    assertEquals(6, plain.body)

    val original = WithToBuilder.builder().id(1).name("a").build()
    val copy = original.toBuilder().name("b").build()
    assertEquals(1, copy.id)
    assertEquals("b", copy.name)

    assertEquals(5, BothAnnotations.builder().id(5).build().id)

    return "OK"
}
