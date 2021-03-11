fun test() {
    val t = 1
    val f = 2
    val a: Int
    a = if (true) t else {<caret>
                           f
    }!!
}