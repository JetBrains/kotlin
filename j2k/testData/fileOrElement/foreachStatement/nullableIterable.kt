internal class A {
    internal var list: List<String>? = null

    internal fun foo() {
        for (e in list!!) {
            println(e)
        }
    }
}