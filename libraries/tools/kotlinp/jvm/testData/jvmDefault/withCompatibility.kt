// JVM_DEFAULT_MODE: no-compatibility
@JvmDefaultWithCompatibility
interface A {
    fun f() {}
}

@JvmDefaultWithCompatibility
interface B : A