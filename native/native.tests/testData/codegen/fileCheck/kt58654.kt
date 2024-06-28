// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: define i64 @"kfun:#foo(){}kotlin.Long"()
fun foo(): Long {
    // CHECK-NOT: @LONG_CACHE
    val data: Map<String, Any> = mapOf()
    return data.getOrElse("id") { 0L } as Long
}
// CHECK: ret i64

inline fun <T> bar(x: T?, f: Boolean): Any {
    when {
        x != null -> return@bar x
        f -> return@bar 0L
        else -> return@bar 1UL
    }
}

// CHECK-LABEL: define i64 @"kfun:#callBar(kotlin.Boolean){}kotlin.ULong
fun callBar(f: Boolean): ULong {
    // CHECK: @LONG_CACHE
    val data: Map<String, Any> = mapOf()
    val x = data["id"]
    return bar(x, f) as ULong
}
// CHECK: ret i64

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    val resultFoo = foo()
    val resultBar = callBar(false)
    return if (resultFoo == 0L && resultBar == 1UL)
        "OK"
    else "FAIL: resultFoo=$resultFoo resultBar=$resultBar"
}
