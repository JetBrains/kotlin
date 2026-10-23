// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -g -Xklib-ir-inliner=disabled
// IGNORE_NATIVE: optimizationMode=OPT

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

// The shared wrapped body must keep the definition file when it is first inlined in A.kt.
// Each caller lambda must keep its own file, and its call site is block() in Wrapped.kt.
// CHECK-LABEL: define i32 @"kfun:{{.*}}#a(kotlin.Int){}kotlin.Int"
// CHECK-SAME: !dbg ![[A_ROOT:[0-9]+]]
// CHECK: add i32 {{.*}}, 2, !dbg ![[A_VALUE:[0-9]+]]
// CHECK-LABEL: define i32 @"kfun:{{.*}}#b(kotlin.Int){}kotlin.Int"
// CHECK-SAME: !dbg ![[B_ROOT:[0-9]+]]
// CHECK: add i32 {{.*}}, 3, !dbg ![[B_VALUE:[0-9]+]]

// CHECK-DAG: ![[A_ROOT]] = distinct !DISubprogram(name: "a",{{.*}}file: ![[A_FILE:[0-9]+]], line: [[A_LINE:[0-9]+]],
// CHECK-DAG: ![[B_ROOT]] = distinct !DISubprogram(name: "b",{{.*}}file: ![[B_FILE:[0-9]+]], line: [[B_LINE:[0-9]+]],
// CHECK-DAG: ![[A_FILE]] = !DIFile(filename: "{{.*}}A.kt",
// CHECK-DAG: ![[B_FILE]] = !DIFile(filename: "{{.*}}B.kt",
// CHECK-DAG: ![[WRAPPED_SCOPE:[0-9]+]] = distinct !DISubprogram(name: "wrapped",{{.*}}file: ![[WRAPPED_FILE:[0-9]+]], line: [[WRAPPED_LINE:[0-9]+]],
// CHECK-DAG: ![[WRAPPED_FILE]] = !DIFile(filename: "{{.*}}Wrapped.kt",
// CHECK-DAG: ![[A_VALUE]] = !DILocation(line: [[A_LINE]], column: 32, scope: ![[A_LAMBDA:[0-9]+]], inlinedAt: ![[A_BLOCK_CALL:[0-9]+]])
// CHECK-DAG: ![[B_VALUE]] = !DILocation(line: [[B_LINE]], column: 32, scope: ![[B_LAMBDA:[0-9]+]], inlinedAt: ![[B_BLOCK_CALL:[0-9]+]])
// CHECK-DAG: ![[A_LAMBDA]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[A_FILE]], line: [[A_LINE]],
// CHECK-DAG: ![[B_LAMBDA]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[B_FILE]], line: [[B_LINE]],
// CHECK-DAG: ![[A_BLOCK_CALL]] = !DILocation(line: [[WRAPPED_LINE]], column: 53, scope: ![[A_WRAPPED_LAMBDA:[0-9]+]], inlinedAt: ![[A_LET_INVOKE:[0-9]+]])
// CHECK-DAG: ![[B_BLOCK_CALL]] = !DILocation(line: [[WRAPPED_LINE]], column: 53, scope: ![[B_WRAPPED_LAMBDA:[0-9]+]], inlinedAt: ![[B_LET_INVOKE:[0-9]+]])
// CHECK-DAG: ![[A_WRAPPED_LAMBDA]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[WRAPPED_FILE]], line: [[WRAPPED_LINE]],
// CHECK-DAG: ![[B_WRAPPED_LAMBDA]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[WRAPPED_FILE]], line: [[WRAPPED_LINE]],
// CHECK-DAG: ![[A_LET_INVOKE]] = !DILocation(line: {{[0-9]+}}, column: {{[0-9]+}}, scope: ![[LET_SCOPE:[0-9]+]], inlinedAt: ![[A_LET_CALL:[0-9]+]])
// CHECK-DAG: ![[B_LET_INVOKE]] = !DILocation(line: {{[0-9]+}}, column: {{[0-9]+}}, scope: ![[LET_SCOPE]], inlinedAt: ![[B_LET_CALL:[0-9]+]])
// CHECK-DAG: ![[LET_SCOPE]] = distinct !DISubprogram(name: "let",{{.*}}
// CHECK-DAG: ![[A_LET_CALL]] = !DILocation(line: [[WRAPPED_LINE]], column: 47, scope: ![[WRAPPED_SCOPE]], inlinedAt: ![[A_WRAPPED_CALL:[0-9]+]])
// CHECK-DAG: ![[B_LET_CALL]] = !DILocation(line: [[WRAPPED_LINE]], column: 47, scope: ![[WRAPPED_SCOPE]], inlinedAt: ![[B_WRAPPED_CALL:[0-9]+]])
// CHECK-DAG: ![[A_WRAPPED_CALL]] = !DILocation(line: [[A_LINE]], column: 22, scope: ![[A_ROOT]])
// CHECK-DAG: ![[B_WRAPPED_CALL]] = !DILocation(line: [[B_LINE]], column: 22, scope: ![[B_ROOT]])
