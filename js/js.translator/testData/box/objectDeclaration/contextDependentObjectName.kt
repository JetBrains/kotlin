interface A {
    fun run()
}

// CHECK_FUNCTION_EXISTS: box$a$1 TARGET_BACKENDS=JS_IR
// CHECK_CLASS_EXISTS: box$a$1 TARGET_BACKENDS=JS_IR_ES6
// CHECK_FUNCTION_EXISTS: box$a$1$run$b$1 TARGET_BACKENDS=JS_IR
// CHECK_CLASS_EXISTS: box$a$1$run$b$1 TARGET_BACKENDS=JS_IR_ES6
fun box(): String {
    var result = "FAILURE"

    val a: A = object : A {
        override fun run() {
            val b = object {
                fun foo() {
                    result = "OK"
                }
            }
            b.foo()
        }
    }

    a.run()
    return result
}
