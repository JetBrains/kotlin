// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=staticPlainEnumEntries=true

enum class PrecreatedEnum {
    Z,
    A,
    M;

    fun marker(): String = name
}

// The statically generated $VALUES array keeps the internal valueOf() order: A, M, Z.
// CHECK-DAG: [[PRECREATED_A:@[0-9]+]] = internal unnamed_addr constant { %struct.ObjHeader, ptr, i32 } { %struct.ObjHeader { ptr {{.*}} }, ptr @{{[0-9]+}}, i32 1 }
// CHECK-DAG: [[PRECREATED_M:@[0-9]+]] = internal unnamed_addr constant { %struct.ObjHeader, ptr, i32 } { %struct.ObjHeader { ptr {{.*}} }, ptr @{{[0-9]+}}, i32 2 }
// CHECK-DAG: [[PRECREATED_Z:@[0-9]+]] = internal unnamed_addr constant { %struct.ObjHeader, ptr, i32 } { %struct.ObjHeader { ptr {{.*}} }, ptr @{{[0-9]+}}, i32 0 }
// CHECK: internal unnamed_addr constant { %struct.ArrayHeader, [3 x ptr] } { %struct.ArrayHeader { ptr {{.*}}, i32 3 }, [3 x ptr] [ptr [[PRECREATED_A]], ptr [[PRECREATED_M]], ptr [[PRECREATED_Z]]] }

// Plain enum entries and EnumEntries are fully precreated, so the enum class needs no runtime initializer.
// CHECK-NOT: define internal void @"kfun:PrecreatedEnum.$init_global#internal"
// CHECK-NOT: call ptr @AllocInstance(ptr noundef @"kclass:PrecreatedEnum"
// CHECK-NOT: call void @"kfun:PrecreatedEnum#<init>(kotlin.String;kotlin.Int){}"
// CHECK-NOT: call ptr @"kfun:kotlin.enums#enumEntries(kotlin.Array<0:0>){0\C2\A7<kotlin.Enum<0:0>>}kotlin.enums.EnumEntries<0:0>"

// Direct enum entry references keep using getter ids over the name-sorted $VALUES array.
// CHECK-LABEL: define ptr @"kfun:PrecreatedEnum#$getEnumAt#static(kotlin.Int){}PrecreatedEnum"
// CHECK: load ptr, ptr @"kvar:PrecreatedEnum.$VALUES#internal"
// CHECK: call ptr @Kotlin_Array_get_without_BoundCheck(ptr %{{[0-9a-z_]+}}, i32 %{{[0-9]+}},

// CHECK-LABEL: define ptr @"kfun:#directPrecreatedZ(){}PrecreatedEnum"
// CHECK: call ptr @"kfun:PrecreatedEnum#$getEnumAt#static(kotlin.Int){}PrecreatedEnum"(i32 2,
fun directPrecreatedZ() = PrecreatedEnum.Z

// CHECK-LABEL: define ptr @"kfun:#directPrecreatedA(){}PrecreatedEnum"
// CHECK: call ptr @"kfun:PrecreatedEnum#$getEnumAt#static(kotlin.Int){}PrecreatedEnum"(i32 0,
fun directPrecreatedA() = PrecreatedEnum.A

// CHECK-LABEL: define ptr @"kfun:#directPrecreatedM(){}PrecreatedEnum"
// CHECK: call ptr @"kfun:PrecreatedEnum#$getEnumAt#static(kotlin.Int){}PrecreatedEnum"(i32 1,
fun directPrecreatedM() = PrecreatedEnum.M

fun box(): String {
    val values = PrecreatedEnum.values()
    val entries = PrecreatedEnum.entries
    if (values.map { it.name }.joinToString() != "Z, A, M") return "FAIL values"
    if (entries.map { it.name }.joinToString() != "Z, A, M") return "FAIL entries"
    if (PrecreatedEnum.valueOf("A") != PrecreatedEnum.A) return "FAIL valueOf"
    if (directPrecreatedZ().marker() != "Z") return "FAIL direct Z"
    if (directPrecreatedA().marker() != "A") return "FAIL direct A"
    if (directPrecreatedM().marker() != "M") return "FAIL direct M"
    return "OK"
}
