fun foo() {
    public interface T: A

    val a = object: A() {}

    fun bar() {
        val b = object: T {}
    }
}
