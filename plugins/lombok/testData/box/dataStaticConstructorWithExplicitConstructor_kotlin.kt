// ISSUE: KT-91284
// FIR_DUMP

import lombok.Data
import kotlin.test.assertEquals

@Data(staticConstructor = "of")
class A(val a: Int, val b: String)

fun box(): String {
    val viaStaticConstructor = A.of(1, "1")
    assertEquals(A.of(1, "1"), viaStaticConstructor)
    return "OK"
}
