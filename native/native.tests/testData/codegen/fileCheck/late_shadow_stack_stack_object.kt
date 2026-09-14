// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: BuildShadowStack
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.native.Retain
import kotlin.native.runtime.GC

class Slot(val id: Int)

// CHECK-LABEL: define {{.*}}@"kfun:#fillLocalArray(){}kotlin.Int"
// CHECK: [[ARRAY:%[0-9a-z_.]+]] = alloca %"local#Array4#internal{{[^"]*}}"
// CHECK: store ptr %shadow_stack_frame, ptr
// CHECK: store ptr [[ARRAY]], ptr %slot_
// CHECK-NOT: Kotlin_gc_stackObject
// CHECK: load ptr, ptr %shadow_stack_frame
@Retain
fun fillLocalArray(): Int {
    val array = arrayOfNulls<Slot>(4)
    for (i in 0 until 4) array[i] = Slot(i)
    GC.collect()
    var sum = 0
    for (slot in array) sum += slot!!.id
    return sum
}

fun box(): String {
    val sum = fillLocalArray()
    return if (sum == 6) "OK" else "FAIL $sum"
}
