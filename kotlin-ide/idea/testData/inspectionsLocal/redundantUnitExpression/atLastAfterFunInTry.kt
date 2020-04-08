fun test(b: Boolean): Unit = try {
    fun a() {}
    <caret>Unit
} catch (e: Exception) {
}