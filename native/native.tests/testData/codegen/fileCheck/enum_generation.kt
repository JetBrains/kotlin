// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

enum class FileCheckEnum(val payload: Int) {
    Z(10),
    A(20),
    M(30),
}

// CHECK-LABEL: define internal void @"kfun:FileCheckEnum.$init_global#internal"

// Backing instances are stored into $VALUES by enum entry name order: A, M, Z.
// The source declarations are then initialized by ordinal order through their getter ids:
// Z -> 2, A -> 0, M -> 1.
// CHECK: call void @UpdateHeapRef(ptr noundef @"kvar:FileCheckEnum.$VALUES#internal"
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 2,
// CHECK: call void @"kfun:FileCheckEnum#<init>(kotlin.String;kotlin.Int;kotlin.Int){}"(ptr %{{[0-9a-z_]+}}, ptr @{{[0-9]+}}, i32 0, i32 10)
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 0,
// CHECK: call void @"kfun:FileCheckEnum#<init>(kotlin.String;kotlin.Int;kotlin.Int){}"(ptr %{{[0-9a-z_]+}}, ptr @{{[0-9]+}}, i32 1, i32 20)
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 1,
// CHECK: call void @"kfun:FileCheckEnum#<init>(kotlin.String;kotlin.Int;kotlin.Int){}"(ptr %{{[0-9a-z_]+}}, ptr @{{[0-9]+}}, i32 2, i32 30)

// $ENTRIES is rebuilt in ordinal order: Z, A, M.
// CHECK: load ptr, ptr @"kvar:FileCheckEnum.$VALUES#internal"
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 2,
// CHECK: call ptr @"kfun:kotlin.native.internal#downcast
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 0,
// CHECK: call ptr @"kfun:kotlin.native.internal#downcast
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 1,
// CHECK: call ptr @"kfun:kotlin.native.internal#downcast
// CHECK: call ptr @"kfun:kotlin.enums#enumEntries(kotlin.Array<0:0>){0\C2\A7<kotlin.Enum<0:0>>}kotlin.enums.EnumEntries<0:0>"
// CHECK: call void @UpdateHeapRef(ptr noundef @"kvar:FileCheckEnum.$ENTRIES#internal"

// Direct enum entry references are lowered to a synthetic getter over the $VALUES array.
// CHECK-LABEL: define ptr @"kfun:FileCheckEnum#$getEnumAt#static(kotlin.Int){}FileCheckEnum"
// CHECK: load ptr, ptr @"kvar:FileCheckEnum.$VALUES#internal"
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 %{{[0-9]+}},

// CHECK-LABEL: define ptr @"kfun:#directZ(){}FileCheckEnum"
// CHECK: call ptr @"kfun:FileCheckEnum#$getEnumAt#static(kotlin.Int){}FileCheckEnum"(i32 2,
fun directZ() = FileCheckEnum.Z

// CHECK-LABEL: define ptr @"kfun:#directA(){}FileCheckEnum"
// CHECK: call ptr @"kfun:FileCheckEnum#$getEnumAt#static(kotlin.Int){}FileCheckEnum"(i32 0,
fun directA() = FileCheckEnum.A

// CHECK-LABEL: define ptr @"kfun:#directM(){}FileCheckEnum"
// CHECK: call ptr @"kfun:FileCheckEnum#$getEnumAt#static(kotlin.Int){}FileCheckEnum"(i32 1,
fun directM() = FileCheckEnum.M

fun box(): String {
    val values = FileCheckEnum.values()
    val entries = FileCheckEnum.entries
    if (values.map { it.name }.joinToString() != "Z, A, M") return "FAIL values"
    if (entries.map { it.name }.joinToString() != "Z, A, M") return "FAIL entries"
    if (directZ().payload != 10 || directA().payload != 20 || directM().payload != 30) return "FAIL direct"
    return "OK"
}
