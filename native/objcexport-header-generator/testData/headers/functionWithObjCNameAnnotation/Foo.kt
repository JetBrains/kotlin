@file:OptIn(ExperimentalObjCName::class)

import kotlin.experimental.ExperimentalObjCName

@kotlin.native.ObjCName("objcTopLevelFunction", "swiftTopLevelFunction")
fun someTopLevelFunction() = ""

class Foo {
    @kotlin.native.ObjCName("objcMemberFunction", "swiftMemberFunction")
    fun someMemberFunction() = Unit
}