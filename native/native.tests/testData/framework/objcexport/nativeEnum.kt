package nativeEnum

import kotlin.native.ObjCEnum
import kotlin.experimental.ExperimentalObjCEnum

@OptIn(kotlin.experimental.ExperimentalObjCEnum::class)
@ObjCEnum("OBJCFoo")
enum class MyKotlinEnum {
    A, B, C
}