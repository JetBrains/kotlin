// ISSUE: KT-89739

import lombok.Builder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Builder
class Mutable {
    @Builder.Default
    var x: Int = 1

    @Builder.Default
    var y: String = "OK"

    @Builder.Default
    var z: List<Boolean> = listOf(false, true)

    companion object {
        fun create(): Mutable {
            return builder().build()
        }
    }
}

fun box(): String {
    val m = Mutable.create()
    assertEquals(1, m.x, "Fail x: ${m.x}")
    assertTrue(!m.z[0] && m.z[1], "Fail z: ${m.z}")
    return m.y
}
