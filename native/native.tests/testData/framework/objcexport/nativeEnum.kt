package nativeEnum

import kotlin.native.ObjCEnum
import kotlin.experimental.ExperimentalObjCEnum

@OptIn(kotlin.experimental.ExperimentalObjCEnum::class)
@ObjCEnum("OBJCFoo", swiftName="SwiftFoo")
enum class MyKotlinEnum {
    ALPHA, COPY, BAR_FOO
}