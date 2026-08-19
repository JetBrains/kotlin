// ISSUE: KT-88571
// WITH_STDLIB
// ONLY_IR_DCE

import kotlinx.serialization.*

// JS/Wasm: The enum entries keep `static_init` alive, and `static_init` is what constructs the companion object.
//   So the associated object survives DCE while its `getInstance` function does not, unless something else
//   keeps `getInstance` reachable directly.
@Serializable
enum class MODE {
    ALL
}

// Calling the reified `serializer<T>()` function resolves the serializer via the associated object
// lookup mechanism, which requires `MODE.Companion.getInstance` to stay reachable so the DCE-kept
// `SerializerFactory` association can still be emitted/used.
fun box(): String {
    val descriptorName = serializer<MODE>().descriptor.serialName
    if (descriptorName != "MODE") return descriptorName
    return if (MODE.ALL.toString() == "ALL") "OK" else "FAIL"
}
