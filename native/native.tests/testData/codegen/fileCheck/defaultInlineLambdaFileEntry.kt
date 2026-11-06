// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xadd-light-debug=enable -Xklib-ir-inliner=disabled

// MODULE: lib
// FILE: Default.kt
fun defaultValue(): Int = 7
inline fun withDefault(block: () -> Int = { defaultValue() }): Int = block()

// MODULE: main(lib)
// FILE: Caller.kt
fun defaultCall(): Int = withDefault()
fun explicitCall(x: Int): Int = withDefault { x + 2 }

// FILE: main.kt
fun box(): String = if (defaultCall() == 7 && explicitCall(1) == 3) "OK" else "FAIL"

// Default lambdas belong to the library; explicit lambdas belong to the caller.
// CHECK: !DICompileUnit(
// CHECK-DAG: !DISubprogram(name: "withDefault",{{.*}}file: ![[DEFAULT_FILE:[0-9]+]], line: [[DEFAULT_LINE:[0-9]+]],
// CHECK-DAG: ![[DEFAULT_FILE]] = !DIFile(filename: "{{.*}}Default.kt",
// CHECK-DAG: !DISubprogram(name: "explicitCall",{{.*}}file: ![[CALLER_FILE:[0-9]+]], line: [[EXPLICIT_CALL_LINE:[0-9]+]],
// CHECK-DAG: ![[CALLER_FILE]] = !DIFile(filename: "{{.*}}Caller.kt",
// CHECK-DAG: !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[DEFAULT_FILE]], line: [[DEFAULT_LINE]],
// CHECK-DAG: !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[CALLER_FILE]], line: [[EXPLICIT_CALL_LINE]],
