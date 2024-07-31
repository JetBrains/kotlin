class MyClass {
    companion object {
        fun ok(): String {
            val oMaker = object {
                fun getO(): String = "O"
            }
            val okMaker = object {
                fun getOk(): String = oMaker.getO() + "K"
            }
            return okMaker.getOk()
        }
    }
}

// TODO: Add similar directives for JS_IR_ES6 but for classes
// CHECK_FUNCTION_EXISTS: MyClass$Companion$ok$oMaker$1 TARGET_BACKENDS=JS_IR
// CHECK_FUNCTION_EXISTS: MyClass$Companion$ok$okMaker$1 TARGET_BACKENDS=JS_IR
fun box(): String {
    return MyClass.ok()
}
