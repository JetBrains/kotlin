// CHECK_ERRORS_AFTER
class C {
    fun f() {
        fun local(any: Any) {
            if (any is String) {
                any.length
            }
        }
    }
}