// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xadd-light-debug=enable -Xklib-ir-inliner=disabled

// MODULE: lib
// FILE: Wrapped.kt
inline fun wrapped(block: () -> Int): Int = 1.let { block() + it }

// MODULE: main(lib)
// FILE: A.kt
fun a(x: Int): Int = wrapped { x + 2 }

// FILE: B.kt
fun b(x: Int): Int = wrapped { x + 3 }

// FILE: main.kt
fun box(): String = if (a(1) == 4 && b(1) == 5) "OK" else "FAIL"

// Library-defined and caller-supplied lambdas keep their own source files and lines.
// CHECK: !DICompileUnit(
// CHECK-DAG: !DISubprogram(name: "wrapped",{{.*}}file: ![[WRAPPED_FILE:[0-9]+]], line: [[WRAPPED_LINE:[0-9]+]],
// CHECK-DAG: ![[WRAPPED_FILE]] = !DIFile(filename: "{{.*}}Wrapped.kt",
// CHECK-DAG: !DISubprogram(name: "a",{{.*}}file: ![[A_FILE:[0-9]+]], line: [[A_LINE:[0-9]+]],
// CHECK-DAG: ![[A_FILE]] = !DIFile(filename: "{{.*}}A.kt",
// CHECK-DAG: !DISubprogram(name: "b",{{.*}}file: ![[B_FILE:[0-9]+]], line: [[B_LINE:[0-9]+]],
// CHECK-DAG: ![[B_FILE]] = !DIFile(filename: "{{.*}}B.kt",
// CHECK-DAG: !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[WRAPPED_FILE]], line: [[WRAPPED_LINE]],
// CHECK-DAG: !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[A_FILE]], line: [[A_LINE]],
// CHECK-DAG: !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[B_FILE]], line: [[B_LINE]],
