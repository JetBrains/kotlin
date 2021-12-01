// CHECK_FUNCTION_EXISTS: box$MyClass TARGET_BACKENDS=JS_IR
// CHECK_FUNCTION_EXISTS: box$MyClass$ok$InternalClass TARGET_BACKENDS=JS_IR
fun box(): String {
    class MyClass {
        fun ok(): String {
            class InternalClass {
                fun getOk(): String = "OK"
            }
            return InternalClass().getOk()
        }
    }
    return MyClass().ok()
}
