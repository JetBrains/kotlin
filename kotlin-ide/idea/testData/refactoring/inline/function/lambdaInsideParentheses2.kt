fun testApply() {
    val i: Int = 42
    val applyInside = i.myApply({toString()})

    val applyTrailing = i.myApply{toString()}
}

public fun <T> T.my<caret>Apply(block: T.() -> Unit): T {
    block()
    return this
}