fun outer() {
    fun <caret>inner1(x: Int, y: Int): Int { val y: Int; }
    fun inner2(x: Int, y: Int): Any { }
}
