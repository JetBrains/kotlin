// ISSUE: KT-89739

import lombok.Builder

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
    return when {
        m.x != 1 -> "Fail x: ${m.x}"
        m.z[0] || !m.z[1] -> "Fail z: ${m.z}"
        else -> m.y
    }
}
