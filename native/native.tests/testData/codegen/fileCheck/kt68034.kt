// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: define i32 @"kfun:#wrapString(kotlin.String){}kotlin.Int
// CHECK-OPT: call {{zeroext i16|i16}} @Kotlin_String_get
// CHECK-DEBUG: call {{zeroext i16|i16}} @"kfun:kotlin.CharSequence#get(kotlin.Int){}kotlin.Char-trampoline"
// CHECK-LABEL: epilogue:
fun wrapString(impl: String) = ifaceHandler(impl)

// CHECK-LABEL: define i32 @"kfun:#wrapStringBuilder(kotlin.CharSequence){}kotlin.Int
// CHECK-OPT: call {{zeroext i16|i16}} @"kfun:kotlin.text.StringBuilder#get(kotlin.Int){}kotlin.Char"
// CHECK-DEBUG: call {{zeroext i16|i16}} @"kfun:kotlin.CharSequence#get(kotlin.Int){}kotlin.Char-trampoline"
// CHECK-LABEL: epilogue:
fun wrapStringBuilder(impl: CharSequence) = ifaceHandler(impl)

inline fun ifaceHandler(seq: CharSequence): Int {
    var sum = 0
    for (idx in 0 ..< seq.length) {
        sum += seq[idx].code
    }
    return sum
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
// CHECK-LABEL: epilogue:
fun box(): String {
    val result1 = wrapString("OK")
    if (result1 != 154)
        return "fail 1: $result1"
    buildString {
        append("OK")
        val result2 = wrapStringBuilder(this)
        if (result2 != 154)
            return "fail 2: $result2"
    }
    return "OK"
}
