import kotlin.native.ObjCEnum
import kotlin.experimental.ExperimentalObjCEnum

@file:OptIn(ExperimentalObjCEnum::class)

@ObjCEnum("OBJCFoo")
enum class Foo {
    A, B, C
}