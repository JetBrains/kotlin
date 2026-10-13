// ISSUE: KT-89739
// WITH_STDLIB

import lombok.Builder
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Builder
class Mutable {
    var x: Int = 0
        get() = 2 * field
        set(arg) { field = 2 * arg }

    var w = 0

    private var y: String by Delegates.observable("") { _, _, _ ->
        w++
    }

    private val zz = mutableListOf<Boolean>()

    var z: List<Boolean>
        get() = zz
        set(arg) {
            zz.addAll(arg)
            zz.add(true)
        }

    fun res() = y

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
    assertEquals(4, m.x, "Fail x: ${m.x}")
    assertEquals(1, m.w, "Fail x: ${m.w}")
    assertTrue(!m.z[0] && m.z[1] && m.z[2], "Fail z: ${m.z}")

    return m.res()
}
