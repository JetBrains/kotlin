// ISSUE: KT-90590
// FIR_DUMP
// FILE: test.kt
import lombok.Builder

@Builder
class RefBodyVar {
    @Builder.Default
    var a: Int = 1

    @Builder.Default
    var b: Int = a + 1

    @Builder.Default
    var c: Int = x + y

    companion object {
        const val x = 10
    }
}

const val y = 20
