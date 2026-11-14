// ISSUE: KT-90709
// IGNORE_BACKEND: JVM

import lombok.Builder
import lombok.Singular
import kotlin.test.assertEquals

@Builder
class BodyVar {
    @Singular var items: List<String> = emptyList()
}

fun box(): String {
    val builder = BodyVar.builder().item("a")
    val first = builder.build()
    builder.item("b")
    // fails, actual first.items = ["a", "b"]
    assertEquals(listOf("a"), first.items)
    return "OK"
}
