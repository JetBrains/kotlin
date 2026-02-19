// WITH_STDLIB

import kotlinx.serialization.*

@Serializable(with = ContextualSerializer::class)
class Ref(
    val id: String,
)

fun box(): String {
    val kind = Ref.serializer().descriptor.kind.toString()
    if (kind != "CONTEXTUAL") return kind
    return "OK"
}
