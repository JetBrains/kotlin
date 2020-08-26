fun a() {
    val a: Int = 42
    a?.l<caret>et {
        println(it)
    }
}