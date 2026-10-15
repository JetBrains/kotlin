// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: LinkBitcodeDependencies
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: targetArchitecture=X64
// DISABLE_NATIVE: targetArchitecture=X86
// DISABLE_NATIVE: targetArchitecture=ARM32
// DISABLE_NATIVE: targetFamily=WATCHOS

// The stack walker of the delta-main scheme follows the chain of frame records through the C++ frames
// of the runtime. The runtime for the supported targets is built so that every function that makes a
// call keeps a frame record: the function attribute says that to the code generator.

// CHECK-DAG: {{define|declare}}{{.*}} void @EnterFrame({{[^\)]*}}) #[[ENTER_FRAME_ATTRS:[0-9]+]]
// CHECK-DAG: attributes #[[ENTER_FRAME_ATTRS]] = {{{.*}}"frame-pointer"="non-leaf"
fun box(): String = "OK"
