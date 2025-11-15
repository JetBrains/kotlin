import kotlin.native.ObjCEnum
import kotlin.experimental.ExperimentalObjCEnum

@file:OptIn(ExperimentalObjCEnum::class)

@ObjCEnum
enum class Foo {
    ALPHA, BAR_FOO, COPY
}