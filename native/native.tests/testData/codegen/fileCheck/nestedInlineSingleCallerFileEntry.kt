// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xadd-light-debug=enable -Xklib-ir-inliner=disabled

// MODULE: lib
// FILE: Wrapped.kt
package external
inline fun wrapped(block: () -> Int): Int = 1.let { block() + it }

// MODULE: main(lib)
// FILE: Main.kt
import external.wrapped
fun box(): String = if (wrapped { 40 + 2 } == 43) "OK" else "FAIL"

// The library lambda keeps its definition file with a single caller.
// CHECK: !DICompileUnit(
// CHECK-DAG: !DISubprogram(name: "wrapped",{{.*}}file: ![[WRAPPED_FILE:[0-9]+]], line: [[WRAPPED_LINE:[0-9]+]],
// CHECK-DAG: ![[WRAPPED_FILE]] = !DIFile(filename: "{{.*}}Wrapped.kt",
// CHECK-DAG: !DISubprogram(name: "box",{{.*}}file: ![[MAIN_FILE:[0-9]+]], line: [[MAIN_LINE:[0-9]+]],
// CHECK-DAG: ![[MAIN_FILE]] = !DIFile(filename: "{{.*}}Main.kt",
// CHECK-DAG: !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[WRAPPED_FILE]], line: [[WRAPPED_LINE]],
// CHECK-DAG: !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[MAIN_FILE]], line: [[MAIN_LINE]],
