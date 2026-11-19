@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

// The arm64e binary links the stubs and sources of the library compiled for arm64e.
// CHECK-DAG: define {{.*}}@arm64e_bitcode_answer_wrapper{{[0-9]+}}() #[[STUB_ATTRIBUTES:[0-9]+]]
// CHECK-DAG: define {{.*}}@compiled() #[[SOURCE_ATTRIBUTES:[0-9]+]]
// CHECK-DAG: attributes #[[STUB_ATTRIBUTES]] = {{.*}}"ptrauth-calls"{{.*}}"target-cpu"="apple-a12"
// CHECK-DAG: attributes #[[SOURCE_ATTRIBUTES]] = {{.*}}"ptrauth-calls"{{.*}}"target-cpu"="apple-a12"

fun foo() = arm64e_bitcode.answer() + arm64e_bitcode.compiled()
