import kotlin.native.ObjCEnum
import kotlin.native.ObjCName
import kotlin.experimental.ExperimentalObjCEnum
import kotlin.experimental.ExperimentalObjCName

@file:OptIn(ExperimentalObjCEnum::class)

// Note that the literal order is not alphabetic on purpose, ensuring that tests fail if we can't rely on the
// order to be preserved for the ordinal value.

@ObjCEnum(name = "ObjCEnumBar")
enum class Bar {
    ALPHA, COPY, BAR_FOO
}

@ObjCEnum
@ObjCName(name = "ObjCNameBaz")
enum class Baz {
    ALPHA, COPY, BAR_FOO
}

@ObjCEnum(name = "ObjCEnumFoo")
@ObjCName(name = "ObjCNameFoo")
enum class Foo {
    ALPHA, COPY, BAR_FOO
}

@ObjCEnum(name = "ObjCEnumFooBar", swiftName = "SwiftEnumFooBar")
@ObjCName(name = "ObjCNameFooBar", swiftName = "SwiftNameFooBar")
enum class FooBar {
    ALPHA, COPY, BAR_FOO
}
