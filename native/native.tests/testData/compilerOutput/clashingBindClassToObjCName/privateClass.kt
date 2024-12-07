@file:kotlin.native.internal.objc.BindClassToObjCName(MyPrivateClass::class, "MyPrivateClassObjC")
import kotlin.native.internal.reflect.objCNameOrNull

private class MyPrivateClass

fun main() = println(MyPrivateClass::class.objCNameOrNull)