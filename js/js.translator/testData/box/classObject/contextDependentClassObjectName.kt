class MyClass {
    companion object {
        fun ok(): String {
            val okMaker = object {
                fun getOk(): String = "OK"
            }
            return okMaker.getOk()
        }
    }
}

// TODO: Add similar directive for JS_IR_ES6 but for class
// CHECK_FUNCTION_EXISTS: MyClass$Companion$ok$okMaker$1 TARGET_BACKENDS=JS_IR
fun box(): String {
    return MyClass.ok()
}
