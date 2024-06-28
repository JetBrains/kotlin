// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:#id(kotlin.Boolean){}kotlin.Boolean"(i1 zeroext %0)
// CHECK-AAPCS-LABEL: define i1 @"kfun:#id(kotlin.Boolean){}kotlin.Boolean"(i1 %0)
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:#id(kotlin.Boolean){}kotlin.Boolean"(i1 zeroext %0)
fun id(arg: Boolean): Boolean {
    return arg
}

// CHECK-DEFAULTABI-LABEL: define signext i8 @"kfun:#id(kotlin.Byte){}kotlin.Byte"(i8 signext %0)
// CHECK-AAPCS-LABEL: define i8 @"kfun:#id(kotlin.Byte){}kotlin.Byte"(i8 %0)
// CHECK-WINDOWSX64-LABEL: define i8 @"kfun:#id(kotlin.Byte){}kotlin.Byte"(i8 %0)
fun id(arg: Byte): Byte {
    return arg
}

// CHECK-DEFAULTABI-LABEL: define zeroext i16 @"kfun:#id(kotlin.Char){}kotlin.Char"(i16 zeroext %0)
// CHECK-AAPCS-LABEL: define i16 @"kfun:#id(kotlin.Char){}kotlin.Char"(i16 %0)
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#id(kotlin.Char){}kotlin.Char"(i16 %0)
fun id(arg: Char): Char {
    return arg
}

// CHECK-DEFAULTABI-LABEL: define signext i16 @"kfun:#id(kotlin.Short){}kotlin.Short"(i16 signext %0)
// CHECK-AAPCS-LABEL: define i16 @"kfun:#id(kotlin.Short){}kotlin.Short"(i16 %0)
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#id(kotlin.Short){}kotlin.Short"(i16 %0)
fun id(arg: Short): Short {
    return arg
}

// CHECK-LABEL: define i32 @"kfun:#id(kotlin.Int){}kotlin.Int"(i32 %0)
fun id(arg: Int): Int {
    return arg
}

// CHECK-LABEL: define i64 @"kfun:#id(kotlin.Long){}kotlin.Long"(i64 %0)
fun id(arg: Long): Long {
    return arg
}

// CHECK-LABEL: define float @"kfun:#id(kotlin.Float){}kotlin.Float"(float %0)
fun id(arg: Float): Float {
    return arg
}

// CHECK-LABEL: define double @"kfun:#id(kotlin.Double){}kotlin.Double"(double %0)
fun id(arg: Double): Double {
    return arg
}

// CHECK-DEFAULTABI-LABEL: define zeroext i8 @"kfun:#id(kotlin.UByte){}kotlin.UByte"(i8 zeroext %0)
// CHECK-AAPCS-LABEL: define i8 @"kfun:#id(kotlin.UByte){}kotlin.UByte"(i8 %0)
// CHECK-WINDOWSX64-LABEL: define i8 @"kfun:#id(kotlin.UByte){}kotlin.UByte"(i8 %0)
fun id(arg: UByte): UByte {
    return arg
}

// CHECK-DEFAULTABI-LABEL: define zeroext i16 @"kfun:#id(kotlin.UShort){}kotlin.UShort"(i16 zeroext %0)
// CHECK-AAPCS-LABEL: define i16 @"kfun:#id(kotlin.UShort){}kotlin.UShort"(i16 %0)
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#id(kotlin.UShort){}kotlin.UShort"(i16 %0)
fun id(arg: UShort): UShort {
    return arg
}

// CHECK-LABEL: define i32 @"kfun:#id(kotlin.UInt){}kotlin.UInt"(i32 %0)
fun id(arg: UInt): UInt {
    return arg
}

// CHECK-LABEL: define i64 @"kfun:#id(kotlin.ULong){}kotlin.ULong"(i64 %0)
fun id(arg: ULong): ULong {
    return arg
}

fun checkPrimitives() {
    // CHECK-DEFAULTABI: call zeroext i1 @"kfun:#id(kotlin.Boolean){}kotlin.Boolean"(i1 zeroext {{.*}})
    // CHECK-AAPCS: call i1 @"kfun:#id(kotlin.Boolean){}kotlin.Boolean"(i1 {{.*}})
    // CHECK-WINDOWSX64: call zeroext i1 @"kfun:#id(kotlin.Boolean){}kotlin.Boolean"(i1 zeroext {{.*}})
    id(true)
    // CHECK-DEFAULTABI: call signext i8 @"kfun:#id(kotlin.Byte){}kotlin.Byte"(i8 signext {{.*}})
    // CHECK-AAPCS: call i8 @"kfun:#id(kotlin.Byte){}kotlin.Byte"(i8 {{.*}})
    // CHECK-WINDOWSX64: call i8 @"kfun:#id(kotlin.Byte){}kotlin.Byte"(i8 {{.*}})
    id(0.toByte())
    // CHECK-DEFAULTABI: call zeroext i16 @"kfun:#id(kotlin.Char){}kotlin.Char"(i16 zeroext {{.*}})
    // CHECK-AAPCS: call i16 @"kfun:#id(kotlin.Char){}kotlin.Char"(i16 {{.*}})
    // CHECK-WINDOWSX64: call i16 @"kfun:#id(kotlin.Char){}kotlin.Char"(i16 {{.*}})
    id('a')
    // CHECK-DEFAULTABI: call signext i16 @"kfun:#id(kotlin.Short){}kotlin.Short"(i16 signext {{.*}})
    // CHECK-AAPCS: call i16 @"kfun:#id(kotlin.Short){}kotlin.Short"(i16 {{.*}})
    // CHECK-WINDOWSX64: call i16 @"kfun:#id(kotlin.Short){}kotlin.Short"(i16 {{.*}})
    id(0.toShort())
    // CHECK: call i32 @"kfun:#id(kotlin.Int){}kotlin.Int"(i32 {{.*}})
    id(0)
    // CHECK: call i64 @"kfun:#id(kotlin.Long){}kotlin.Long"(i64 {{.*}})
    id(0L)
    // CHECK: call float @"kfun:#id(kotlin.Float){}kotlin.Float"(float {{.*}})
    id(0.5f)
    // CHECK: call double @"kfun:#id(kotlin.Double){}kotlin.Double"(double {{.*}})
    id(0.5)
    // CHECK-DEFAULTABI: call zeroext i8 @"kfun:#id(kotlin.UByte){}kotlin.UByte"(i8 zeroext {{.*}})
    // CHECK-AAPCS: call i8 @"kfun:#id(kotlin.UByte){}kotlin.UByte"(i8 {{.*}})
    // CHECK-WINDOWSX64: call i8 @"kfun:#id(kotlin.UByte){}kotlin.UByte"(i8 {{.*}})
    id(0.toUByte())
    // CHECK-DEFAULTABI: call zeroext i16 @"kfun:#id(kotlin.UShort){}kotlin.UShort"(i16 zeroext {{.*}})
    // CHECK-AAPCS: call i16 @"kfun:#id(kotlin.UShort){}kotlin.UShort"(i16 {{.*}})
    // CHECK-WINDOWSX64: call i16 @"kfun:#id(kotlin.UShort){}kotlin.UShort"(i16 {{.*}})
    id(0.toUShort())
    // CHECK: call i32 @"kfun:#id(kotlin.UInt){}kotlin.UInt"(i32 {{.*}})
    id(15u)
    // CHECK: call i64 @"kfun:#id(kotlin.ULong){}kotlin.ULong"(i64 {{.*}})
    id(15uL)
}

value class CharWrapper(val ch: Char)

// CHECK-DEFAULTABI-LABEL: define zeroext i16 @"kfun:#id(CharWrapper){}CharWrapper"(i16 zeroext %0)
// CHECK-AAPCS-LABEL: define i16 @"kfun:#id(CharWrapper){}CharWrapper"(i16 %0)
// CHECK-WINDOWSX64-LABEL: define i16 @"kfun:#id(CharWrapper){}CharWrapper"(i16 %0)
fun id(arg: CharWrapper): CharWrapper {
    return arg
}

// Check that value classes doesn't affect parameter attributes

fun checkInlineClasses() {
    // CHECK-DEFAULTABI: call zeroext i16 @"kfun:#id(CharWrapper){}CharWrapper"(i16 zeroext {{.*}})
    // CHECK-AAPCS: call i16 @"kfun:#id(CharWrapper){}CharWrapper"(i16 {{.*}})
    // CHECK-WINDOWSX64: call i16 @"kfun:#id(CharWrapper){}CharWrapper"(i16 {{.*}})
    id(CharWrapper('c'))
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#nullableId(kotlin.Byte?){}kotlin.Byte?"(%struct.ObjHeader* %0, %struct.ObjHeader** %1)
fun nullableId(arg: Byte?): Byte? {
    return arg
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#nullableId(CharWrapper?){}CharWrapper?"(%struct.ObjHeader* %0, %struct.ObjHeader** %1)
fun nullableId(arg: CharWrapper?): CharWrapper? {
    return arg
}

// Check that we don't pass primitive-specific attributes to their boxes
fun checkBoxes() {
    // CHECK: call %struct.ObjHeader* @"kfun:#nullableId(kotlin.Byte?){}kotlin.Byte?"(%struct.ObjHeader* {{.*}}, %struct.ObjHeader** {{.*}})
    nullableId(1.toByte())
    // CHECK: call %struct.ObjHeader* @"kfun:#nullableId(CharWrapper?){}CharWrapper?"(%struct.ObjHeader* {{.*}}, %struct.ObjHeader** {{.*}})
    nullableId(CharWrapper('a'))
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    checkPrimitives()
    checkInlineClasses()
    checkBoxes()
    return "OK"
}