fun foo(fe: Int) {
    println(fe)
}

fun bar(br: Boolean) {
    val fe = 0
    if (br) {
        foo(fe)
    }
    else {
        <caret>foo(11)
    }
}
