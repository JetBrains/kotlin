@file:OptIn(ExperimentalObjCEnum::class)

import kotlin.native.ObjCEnum
import kotlin.native.ObjCName
import kotlin.experimental.ExperimentalObjCEnum
import kotlin.experimental.ExperimentalObjCName

@ObjCEnum
enum class Foo {
    ALPHA, COPY, BAR_FOO
}