// JVM_DEFAULT_MODE: no-compatibility
// WITH_STDLIB

@JvmDefaultWithCompatibility
interface A {
    fun f() {}
}

@JvmDefaultWithCompatibility
interface B : A