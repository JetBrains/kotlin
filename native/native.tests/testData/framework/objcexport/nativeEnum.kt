package nativeEnum

import kotlin.native.ObjCEnum
import kotlin.experimental.ExperimentalObjCEnum

// Note that the entries are explicitly not in alphabetical order to show that the compiler doesn't
// sort them somewhere.
@OptIn(kotlin.experimental.ExperimentalObjCEnum::class)
@ObjCEnum("OBJCFoo", swiftName="SwiftFoo")
enum class MyKotlinEnum {
    ALPHA,
    @ObjCEnum.EntryName("renamed")
    ORIGINAL,
    COPY,
}
