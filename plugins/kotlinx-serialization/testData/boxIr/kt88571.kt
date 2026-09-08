// ISSUE: KT-88571
// WITH_STDLIB
// ONLY_IR_DCE

import kotlinx.serialization.*

// JS/Wasm: The enum entries keep `static_init` alive, and `static_init` is what constructs the companion object.
//   So the associated object survives DCE while its `getInstance` function does not.
@Serializable
enum class MODE {
    ALL
}

fun box(): String {
    // JS/Wasm: Touches an entry only, never the MODE companion, so `MODE$Companion$getInstance` stays unreachable.
    return if (MODE.ALL.toString() == "ALL") "OK" else "FAIL"
}
