// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: LinkBitcodeDependencies

// Rutime bitcode for this targets is generated with "generic" target_cpu
// DISABLE_NATIVE: targetFamily=ANDROID && targetArchitecture=ARM64
// DISABLE_NATIVE: targetFamily=ANDROID && targetArchitecture=X86
// DISABLE_NATIVE: targetFamily=LINUX && targetArchitecture=ARM64

// CHECK-DAG: {{define|declare}}{{.*}} void @EnterFrame({{[^\)]*}}) #[[ENTER_FRAME_ATTRS:[0-9]+]]

// CHECK-DAG: define ptr @"kfun:#box(){}kotlin.String"(ptr %0) #[[BOX_ATTRS:[0-9]+]]
fun box(): String = "OK"

// CHECK-DAG: attributes #[[ENTER_FRAME_ATTRS]] = {{{.*}}"target-cpu"="[[RT_CPU:[^"]+]]" "target-features"="[[RT_FEATURES:[^"]+]]"

// CHECK-DAG: attributes #[[BOX_ATTRS]] = {{{.*}}"target-cpu"="[[RT_CPU]]" "target-features"="{{[^"]*}}[[RT_FEATURES]]{{[^"]*}}"
