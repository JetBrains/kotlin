class A {
    var list: List<String>? = null

    fun foo() {
        for (e in list!!) {
            System.out.println(e)
        }
    }
}