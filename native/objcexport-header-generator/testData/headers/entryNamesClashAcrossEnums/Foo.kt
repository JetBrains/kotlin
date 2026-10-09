import kotlin.native.ObjCEnum
import kotlin.experimental.ExperimentalObjCEnum

@file:OptIn(ExperimentalObjCEnum::class)

@ObjCEnum(name = "Foo") enum class A {
    BAR_BAZ
}

@ObjCEnum(name = "FooBar") enum class B {
    BAZ // Clashes with FooBarBaz from the above enum. Should be mangled.
}
