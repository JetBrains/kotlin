// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// MODULE: lib
// FILE: A.kt
class A {

    // CHECK-DEBUG: define ptr @"kfun:A#internalInlineMethod(){}kotlin.String"
    // CHECK-DEBUG: call void @"kfun:A.object-1.<init>#internal"
    internal inline fun internalInlineMethod() = object {
        fun run() = "OK"
    }.run()

    // CHECK: define ptr @"kfun:A#publicMethod(){}kotlin.String"
    // CHECK-COUNT-2: call void @"kfun:A.object-1.<init>#internal"
    fun publicMethod() = internalInlineMethod() + internalInlineMethod()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    // Test that the local class is extracted and reused in each inline function call site
    // CHECK-COUNT-2: call void @"kfun:A.object-1.<init>#internal"
    A().internalInlineMethod().let {
        if (it != "OK") return it
    }
    A().internalInlineMethod().let {
        if (it != "OK") return it
    }

    A().publicMethod().let {
        if (it != "OKOK") return it
    }
    return "OK"
}
