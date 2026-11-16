// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO

// KT-91549: companion accesses must not go through a static initializer getter
// when the superclasses have nothing to initialize.

// Stdlib functions are emitted before the user code, so this check goes first.
// CHECK-LABEL: define ptr @"kfun:kotlin.collections.ArrayList#get(kotlin.Int){}1:0"
// CHECK-NOT: kotlin.collections.AbstractList#<get-$companion>
// CHECK: {{^}}epilogue:

// CHECK-LABEL: define float @"kfun:#floatFromBits(kotlin.Int){}kotlin.Float"
// CHECK-NOT: kotlin.Float#<get-$companion>
fun floatFromBits(x: Int) = Float.fromBits(x)
// CHECK: {{^}}epilogue:

// CHECK-LABEL: define double @"kfun:#doubleFromBits(kotlin.Long){}kotlin.Double"
// CHECK-NOT: kotlin.Double#<get-$companion>
fun doubleFromBits(x: Long) = Double.fromBits(x)
// CHECK: {{^}}epilogue:

fun box(): String {
    if (floatFromBits(0x3f800000) != 1.0f) return "FAIL 1"
    if (doubleFromBits(0x3ff0000000000000L) != 1.0) return "FAIL 2"
    val list = arrayListOf("O", "K")
    return list[0] + list[1]
}
