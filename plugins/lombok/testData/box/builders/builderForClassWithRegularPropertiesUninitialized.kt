// ISSUE: KT-89739

import lombok.Builder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Builder
class Mutable {
    var x: Int = 0

    lateinit var y: String

    lateinit var z: List<Boolean>

    companion object {
        fun create(): Mutable {
            return builder()
                .x(1)
                .y("OK")
                .z(listOf(false, true))
                .build()
        }
    }
}

fun box(): String {
    val m = Mutable.create()
    assertEquals(1, m.x, "Fail x: ${m.x}")
    assertTrue(!m.z[0] && m.z[1], "Fail z: ${m.z}")
    return m.y
}
