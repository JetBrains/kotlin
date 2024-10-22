// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// MODULE: lib
// FILE: A.kt
class A {

    // CHECK-DEBUG: define ptr @"kfun:A#internalInlineMethod(kotlin.Any?){}kotlin.String"
    // CHECK-DEBUG: call void @"kfun:A.A$internalInlineMethod$1.<init>#internal"
    internal inline fun internalInlineMethod(random: Any?) = object {
        fun run() = "OK"
    }.run()

    // CHECK: define ptr @"kfun:A#publicMethod(){}kotlin.String"
    // CHECK: call void @"kfun:A.A$publicMethod$$inlined$internalInlineMethod$1.<init>#internal"
    // CHECK: call void @"kfun:A.A$publicMethod$$inlined$internalInlineMethod$2.<init>#internal"
    fun publicMethod() = internalInlineMethod(1) + internalInlineMethod(2)
}

// MODULE: main()(lib)
// FILE: main.kt

// CHECK: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    // Test that the local class is not extracted and not reused in each inline function call site
    // CHECK: call void @"kfun:box$$inlined$internalInlineMethod$1.<init>#internal"
    A().internalInlineMethod(3).let {
        if (it != "OK") return it
    }
    // CHECK: call void @"kfun:box$$inlined$internalInlineMethod$2.<init>#internal"
    A().internalInlineMethod(4).let {
        if (it != "OK") return it
    }

    A().publicMethod().let {
        if (it != "OKOK") return it
    }
    return "OK"
}
