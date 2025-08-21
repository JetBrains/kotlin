// JVM_DEFAULT_MODE: enable
// WITH_STDLIB

@JvmDefaultWithoutCompatibility
interface A {
    fun f() {}
}

interface B : A
