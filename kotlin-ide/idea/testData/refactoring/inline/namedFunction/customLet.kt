public inline fun <T, R> T.myLet(block: (T) -> R): R {

    return block(this)
}

fun a() {
    val a: Int = 42
    a?.myLet<caret> {
        println(it)
    }
}