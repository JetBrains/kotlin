// CHECK_OPTIMIZED_JS

// MODULE: lib
// FILE: lib.kt

fun lib_fn(x: Int) = x + 1

fun getRef(): (Int) -> Int = ::lib_fn

// MODULE: main(lib)
// FILE: main.kt

// EXPECT_GENERATED_JS: function=getRef expect=crossModule.getRef.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=getRef expect=crossModule.getRef.es6.js TARGET_BACKENDS=JS_IR_ES6

fun box(): String {
    val f = getRef()
    if (f(10) != 11) return "fail invoke"
    return "OK"
}
