// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: ValueClass
// FILE: ValueClass.kt

class Foo(val x: Int)
value class Bar(val foo: Foo)
