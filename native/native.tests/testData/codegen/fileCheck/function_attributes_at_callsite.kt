// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK: declare void @ThrowException(ptr noundef) #[[THROW_EXCEPTION_DECLARATION_ATTRIBUTES:[0-9]+]]

// CHECK: void @"kfun:#flameThrower(){}kotlin.Nothing"() #[[FLAME_THROWER_DECLARATION_ATTRIBUTES:[0-9]+]]
fun flameThrower(): Nothing {
    // CHECK: call void @ThrowException(ptr {{.*}}) #[[THROW_EXCEPTION_CALLSITE_ATTRIBUTES:[0-9]+]]
    throw Throwable("🔥")
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {

    // CHECK: invoke void @"kfun:#flameThrower(){}kotlin.Nothing"() #[[FLAME_THROWER_CALLSITE_ATTRIBUTES:[0-9]+]]
    try {
        flameThrower()
    } catch (e: Throwable) {
        return "OK"
    }
    return "FAIL: Uncaught exception"
}

// CHECK-DAG: attributes #[[THROW_EXCEPTION_DECLARATION_ATTRIBUTES]] = {{{.*}}noreturn{{.*}}}
// CHECK-DAG: attributes #[[THROW_EXCEPTION_CALLSITE_ATTRIBUTES]] = {{{.*}}noreturn{{.*}}}
// CHECK-DAG: attributes #[[FLAME_THROWER_DECLARATION_ATTRIBUTES]] = {{{.*}}noreturn{{.*}}}
// CHECK-DAG: attributes #[[FLAME_THROWER_CALLSITE_ATTRIBUTES]] = {{{.*}}noreturn{{.*}}}