import kotlin.native.ObjCEnum
import kotlin.native.ObjCName
import kotlin.experimental.ExperimentalObjCEnum
import kotlin.experimental.ExperimentalObjCName

@file:OptIn(ExperimentalObjCEnum::class)

@ObjCEnum
enum class Foo {
    ALPHA_BETA,
    ALPHA,
    COPY,
    @ObjCName("fooBarObjC") FOO_BAR,
    @ObjCName(name = "barFooObjC", swiftName = "barFooSwift") BAR_FOO,
}