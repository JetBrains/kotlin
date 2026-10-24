// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -g -Xklib-ir-inliner=disabled
// IGNORE_NATIVE: optimizationMode=OPT

// MODULE: lib
// FILE: Wrapped.kt
package external
inline fun wrapped(block: () -> Int): Int = 1.let { block() + it }

// MODULE: main(lib)
// FILE: Main.kt
import external.wrapped
fun box(): String = if (wrapped { 40 + 2 } == 43) "OK" else "FAIL"

// One caller is enough to misattribute the library lambda, even without IC.
// CHECK-LABEL: define {{.*}} @"kfun:{{.*}}#box(){}kotlin.String"
// CHECK-SAME: !dbg ![[ROOT:[0-9]+]]
// CHECK: add i32 {{.*}}, !dbg ![[SUM:[0-9]+]]

// CHECK-DAG: ![[ROOT]] = distinct !DISubprogram(name: "box",{{.*}}file: ![[MAIN_FILE:[0-9]+]], line: [[MAIN_LINE:[0-9]+]],
// CHECK-DAG: ![[MAIN_FILE]] = !DIFile(filename: "{{.*}}Main.kt",
// CHECK-DAG: ![[WRAPPED_SCOPE:[0-9]+]] = distinct !DISubprogram(name: "wrapped",{{.*}}file: ![[WRAPPED_FILE:[0-9]+]], line: [[WRAPPED_LINE:[0-9]+]],
// CHECK-DAG: ![[WRAPPED_FILE]] = !DIFile(filename: "{{.*}}Wrapped.kt",
// CHECK-DAG: ![[SUM]] = !DILocation(line: [[WRAPPED_LINE]], column: 53, scope: ![[LIBRARY_LAMBDA:[0-9]+]], inlinedAt: ![[LET_INVOKE:[0-9]+]])
// CHECK-DAG: ![[LIBRARY_LAMBDA]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[WRAPPED_FILE]], line: [[WRAPPED_LINE]],
// CHECK-DAG: ![[CALLER_LAMBDA:[0-9]+]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[MAIN_FILE]], line: [[MAIN_LINE]],
// CHECK-DAG: !{{[0-9]+}} = !DILocation(line: [[MAIN_LINE]], column: 35, scope: ![[CALLER_LAMBDA]], inlinedAt: ![[SUM]])
// CHECK-DAG: ![[LET_INVOKE]] = !DILocation(line: {{[0-9]+}}, column: {{[0-9]+}}, scope: ![[LET_SCOPE:[0-9]+]], inlinedAt: ![[LET_CALL:[0-9]+]])
// CHECK-DAG: ![[LET_SCOPE]] = distinct !DISubprogram(name: "let",{{.*}}
// CHECK-DAG: ![[LET_CALL]] = !DILocation(line: [[WRAPPED_LINE]], column: 47, scope: ![[WRAPPED_SCOPE]], inlinedAt: ![[WRAPPED_CALL:[0-9]+]])
// CHECK-DAG: ![[WRAPPED_CALL]] = !DILocation(line: [[MAIN_LINE]], column: 25, scope: ![[ROOT]])
