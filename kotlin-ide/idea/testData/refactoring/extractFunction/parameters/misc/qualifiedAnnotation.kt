// SIBLING:
class MyClass {
    fun test() {
        <selection>@[P] val t: Int = 1
        t</selection>
    }

    public annotation class P
}