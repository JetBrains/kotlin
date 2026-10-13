// ISSUE: KT-89739
// WITH_STDLIB

import lombok.Builder
import kotlin.properties.Delegates

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
    return when {
        m.x != 4 -> "Fail x: ${m.x}"
        m.w != 1 -> "Fail w: ${m.w}"
        m.z[0] || !m.z[1] || !m.z[2] -> "Fail z: ${m.z}"
        else -> m.res()
    }
}
