// WITH_STDLIB

// An interface member can never be exposed, so a 'fun interface' whose single abstract method takes a value
// class keeps only its mangled form. The mangled name is dropped from KAPT stubs, which leaves the interface
// empty - a processor then cannot see that the type has an abstract member at all. 'ControlTransform' is the
// control: with no value class in its signature the member stays in the stub.

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

fun interface Transform {
    fun apply(id: Id): Id
}
