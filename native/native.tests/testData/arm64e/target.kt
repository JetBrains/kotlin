// CHECK: target triple = "arm64e-apple-ios{{[0-9.]+}}"

// The Objective-C part of the runtime is compiled for arm64e.
// CHECK: @"OBJC_CLASS_$_NSObject.ptrauth" = private constant

// CHECK: define {{.*}}@"kfun:{{.*}}foo(){}kotlin.Int"() #[[ATTRIBUTES:[0-9]+]]
// CHECK: attributes #[[ATTRIBUTES]] = {{.*}}"target-cpu"="apple-a12" "target-features"="+aes,+ccidx,+complxnum,+crc,+fp-armv8,+fullfp16,+jsconv,+lse,+neon,+pauth,+perfmon,+ras,+rcpc,+rdm,+sha2,+v8.1a,+v8.2a,+v8.3a,+v8a"

fun foo() = 42
