fun test() {
    class Test{
        operator fun contains(a: Int) : Boolean = true
    }
    val test = Test()
    if (!test.<caret>contains(1)) return
}
