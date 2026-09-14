// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

// CHECK: declare void @__cxa_end_catch(){{.*}} #[[CXA_END_CATCH_ATTRS:[0-9]+]]
// CHECK-NOT: attributes #[[CXA_END_CATCH_ATTRS]] = {{.*}}"kotlin-gc-frame"

fun mayThrow(x: Int): Int {
    if (x == 0) throw IllegalArgumentException()
    return x
}

fun box(): String {
    try {
        mayThrow(0)
    } catch (e: IllegalArgumentException) {
        return "OK"
    }
    return "FAIL"
}
