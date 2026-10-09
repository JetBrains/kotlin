// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: cacheMode=STATIC_ONLY_DIST
// DISABLE_NATIVE: cacheMode=STATIC_EVERYWHERE
// DISABLE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// DISABLE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true

// CHECK-LABEL: define internal ptr @convertFunction{{[0-9]+}}(ptr %0)
// CHECK: {{call|invoke}} ptr @Kotlin_objc_retainBlock_inNative
// CHECK-NOT: ret ptr
// CHECK: call void @Kotlin_gc_keepAlive(ptr addrspace(1)
// CHECK: ret ptr %
fun box(): String = "OK"
