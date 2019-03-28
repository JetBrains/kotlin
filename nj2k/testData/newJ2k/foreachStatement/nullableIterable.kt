internal class A {
    var list: List<String?>? = null

    fun foo() {
        for (e in list!!) {
            println(e)
        }
    }
}