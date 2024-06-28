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

// CHECK_FUNCTION_EXISTS: MyClass$Companion$ok$oMaker$1 TARGET_BACKENDS=JS_IR
// CHECK_FUNCTION_EXISTS: MyClass$Companion$ok$okMaker$1 TARGET_BACKENDS=JS_IR
fun box(): String {
    return MyClass.ok()
}
