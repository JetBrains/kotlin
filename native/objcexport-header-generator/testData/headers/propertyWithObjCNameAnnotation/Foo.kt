@file:OptIn(ExperimentalObjCName::class)

import kotlin.experimental.ExperimentalObjCName

@kotlin.native.ObjCName("objcValName", "swiftValName")
val valProperty = 42

@kotlin.native.ObjCName("objcVaRName", "swiftVaRName")
var varProperty = 42