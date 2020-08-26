class One {
    val field = "field"
    fun testFoo(field: String) {
        val field1 = this.field
        val field2 = field
    }
}

fun check() {
    val one = One()
    one.<caret>testFoo("arg")
}