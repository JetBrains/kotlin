// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -g -Xklib-ir-inliner=disabled
// IGNORE_NATIVE: optimizationMode=OPT

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

// A default lambda is defined in the callee's file; an explicit lambda is defined in the caller's file.
// CHECK-LABEL: define i32 @"kfun:{{.*}}#defaultCall(){}kotlin.Int"
// CHECK-SAME: !dbg ![[DEFAULT_ROOT:[0-9]+]]
// CHECK: call i32 @"kfun:{{.*}}#defaultValue(){}kotlin.Int"(), !dbg ![[DEFAULT_VALUE:[0-9]+]]
// CHECK-LABEL: define i32 @"kfun:{{.*}}#explicitCall(kotlin.Int){}kotlin.Int"
// CHECK-SAME: !dbg ![[EXPLICIT_ROOT:[0-9]+]]
// CHECK: add i32 {{.*}}, 2, !dbg ![[EXPLICIT_VALUE:[0-9]+]]

// CHECK-DAG: ![[DEFAULT_ROOT]] = distinct !DISubprogram(name: "defaultCall",{{.*}}file: ![[CALLER_FILE:[0-9]+]], line: [[DEFAULT_CALL_LINE:[0-9]+]],
// CHECK-DAG: ![[EXPLICIT_ROOT]] = distinct !DISubprogram(name: "explicitCall",{{.*}}file: ![[CALLER_FILE]], line: [[EXPLICIT_CALL_LINE:[0-9]+]],
// CHECK-DAG: ![[CALLER_FILE]] = !DIFile(filename: "{{.*}}Caller.kt",
// CHECK-DAG: ![[CALLEE_SCOPE:[0-9]+]] = distinct !DISubprogram(name: "withDefault",{{.*}}file: ![[DEFAULT_FILE:[0-9]+]], line: [[DEFAULT_LINE:[0-9]+]],
// CHECK-DAG: ![[DEFAULT_FILE]] = !DIFile(filename: "{{.*}}Default.kt",
// CHECK-DAG: ![[DEFAULT_VALUE]] = !DILocation(line: [[DEFAULT_LINE]], column: 45, scope: ![[DEFAULT_LAMBDA:[0-9]+]], inlinedAt: ![[DEFAULT_INVOKE:[0-9]+]])
// CHECK-DAG: ![[DEFAULT_LAMBDA]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[DEFAULT_FILE]], line: [[DEFAULT_LINE]],
// CHECK-DAG: ![[DEFAULT_INVOKE]] = !DILocation(line: [[DEFAULT_LINE]], column: 70, scope: ![[CALLEE_SCOPE]], inlinedAt: ![[DEFAULT_CALL:[0-9]+]])
// CHECK-DAG: ![[DEFAULT_CALL]] = !DILocation(line: [[DEFAULT_CALL_LINE]], column: 26, scope: ![[DEFAULT_ROOT]])
// CHECK-DAG: ![[EXPLICIT_VALUE]] = !DILocation(line: [[EXPLICIT_CALL_LINE]], column: 47, scope: ![[EXPLICIT_LAMBDA:[0-9]+]], inlinedAt: ![[EXPLICIT_INVOKE:[0-9]+]])
// CHECK-DAG: ![[EXPLICIT_LAMBDA]] = distinct !DISubprogram(name: "<inlined-lambda>",{{.*}}file: ![[CALLER_FILE]], line: [[EXPLICIT_CALL_LINE]],
// CHECK-DAG: ![[EXPLICIT_INVOKE]] = !DILocation(line: [[DEFAULT_LINE]], column: 70, scope: ![[CALLEE_SCOPE]], inlinedAt: ![[EXPLICIT_CALL:[0-9]+]])
// CHECK-DAG: ![[EXPLICIT_CALL]] = !DILocation(line: [[EXPLICIT_CALL_LINE]], column: 33, scope: ![[EXPLICIT_ROOT]])
