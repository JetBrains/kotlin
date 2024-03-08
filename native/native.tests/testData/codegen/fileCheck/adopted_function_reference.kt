// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

inline fun foo(x: () -> Unit): String {
    x()
    return "OK"
}

fun String.id(s: String = this, vararg xs: Int): String = s

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK-LABEL: entry
    // CHECK-NOT: call ptr @AllocInstance
    // CHECK-NOT: alloca
    return foo("Fail"::id)
    // CHECK-LABEL: epilogue:
}