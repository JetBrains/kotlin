// Review of the PR around KT-91284: with `access = AccessLevel.NONE` Lombok generates nothing at all,
// neither the static factory nor the private constructor behind it.
// Before commit "Lombok: generate both Java private constructor & static factory if needed" nothing was generated
// (`accessLevel.toVisibility(...) ?: return` came first); now `privateConstructorMode` skips the access level check
// and emits a private no-args constructor into the bytecode of the Kotlin class.

import lombok.AccessLevel
import lombok.NoArgsConstructor

@NoArgsConstructor(staticName = "of", access = AccessLevel.NONE)
class K(val x: Int)

fun box(): String {
    val parameterCounts = K::class.java.declaredConstructors.map { it.parameterCount }.sorted()
    return if (parameterCounts == listOf(1)) "OK" else "Fail: $parameterCounts"
}
