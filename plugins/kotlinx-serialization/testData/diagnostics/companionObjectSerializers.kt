// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.serialization.*

class Bar

<!COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED!>@Serializable<!>
class Foo1 {
    @Serializer(Foo1::class)
    companion object
}

@Serializable
class Foo2 {
    <!COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS!>@Serializer(Bar::class)<!>
    companion object
}

@Serializer(Foo3::class)
object Foo3Ser

@Serializable(Foo3Ser::class)
class Foo3 {
    <!COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS!>@Serializer(Bar::class)<!>
    companion object
}

@Serializable(Foo4.Companion::class)
class Foo4 {
    @Serializer(Foo4::class)
    companion object
}

class NonSerializableFoo {
    <!COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS!>@Serializer(Bar::class)<!>
    companion object
}