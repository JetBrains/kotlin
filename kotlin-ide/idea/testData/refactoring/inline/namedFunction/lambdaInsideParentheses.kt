fun testWith() {
    val i: Int = 42
    val withInside = myWith(i, { toString() })
    val withTrailing = myWith(i) { toString() }
}

public fun <T, R> myW<caret>ith(receiver: T, block: T.() -> R): R {
    return receiver.block()
}