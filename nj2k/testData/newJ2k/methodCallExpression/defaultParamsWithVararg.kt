object Test {
    fun checkState(condition: Boolean, message: String?, vararg args: Any?) {}
    fun checkState(condition: Boolean) {
        checkState(condition, "condition not met")
    }
}
