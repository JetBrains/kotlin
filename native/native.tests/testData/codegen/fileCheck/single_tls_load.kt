// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: WriteBitcodeFile
// The test checks, that in an optimized build TLS access is optimized, no promises for other builds
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: optimizationMode=DEBUG

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
import kotlin.native.Retain

class Wrapper(x: Int)

// CHECK-LABEL: define {{.*}}ptr @"kfun:#f(kotlin.Int;kotlin.String){}kotlin.String"
@Retain
fun f(x: Int, s: String): String {
    // CHECK: _ZN6kotlin2mm14ThreadRegistry22currentThreadDataNode_E
    // CHECK-NOT: _ZN6kotlin2mm14ThreadRegistry22currentThreadDataNode_E
    if (x < 0) throw IllegalStateException()
    if (x > 0) return f(x - 1, s)
    val b = Wrapper(2)
    val a = listOf(x, x, Wrapper(1), 2, x)
    return buildString {
        for (i in a) { appendLine("$s i") }
    }
// CHECK-LABEL: epilogue:
}

fun box(): String {
    val result = f(10, "123456")
    return if (result == "123456 i\n" +
        "123456 i\n" +
        "123456 i\n" +
        "123456 i\n" +
        "123456 i\n")
        "OK"
    else "FAIL: $result"
}
