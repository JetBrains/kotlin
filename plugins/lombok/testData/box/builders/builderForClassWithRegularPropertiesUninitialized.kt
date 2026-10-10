// ISSUE: KT-89739

import lombok.Builder

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
    return when {
        m.x != 1 -> "Fail x: ${m.x}"
        m.z[0] || !m.z[1] -> "Fail z: ${m.z}"
        else -> m.y
    }
}
