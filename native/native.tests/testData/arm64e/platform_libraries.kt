@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSStringFromSelector

// The arm64e binary links the stubs of the platform libraries compiled for arm64e.
// CHECK: define {{.*}}@{{[A-Za-z0-9_]*}}NSStringFromSelector{{[A-Za-z0-9_]*}}({{.*}}) #[[ATTRIBUTES:[0-9]+]]
// CHECK: attributes #[[ATTRIBUTES]] = {{.*}}"ptrauth-calls"{{.*}}"target-cpu"="apple-a12"

fun selectorName() = NSStringFromSelector(NSSelectorFromString("description"))
