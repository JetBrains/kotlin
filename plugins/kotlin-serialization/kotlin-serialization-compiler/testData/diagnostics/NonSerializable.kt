// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

class NonSerializable

@Serializable
class Basic(val foo: <!PLUGIN_ERROR("Serializer has not been found for type 'NonSerializable'. To use context serializer as fallback, explicitly annotate type or property with @ContextualSerialization")!>NonSerializable<!>)

@Serializable
class Inside(val foo: List<<!PLUGIN_ERROR("Serializer has not been found for type 'NonSerializable'. To use context serializer as fallback, explicitly annotate type or property with @ContextualSerialization")!>NonSerializable<!>>)

@Serializable
class WithImplicitType {
    <!PLUGIN_ERROR("Serializer has not been found for type 'NonSerializable'. To use context serializer as fallback, explicitly annotate type or property with @ContextualSerialization")!>val foo = NonSerializable()<!>
}