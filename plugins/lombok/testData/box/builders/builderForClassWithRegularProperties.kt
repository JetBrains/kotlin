// ISSUE: KT-89739
// IGNORE_BACKEND: JVM
// (currently all properties are left unchanged, as given in their initializers)

import lombok.Builder;

@Builder
class Mutable {
    var x: Int = 0

    var y: String = ""

    var z: List<Boolean> = emptyList()

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
