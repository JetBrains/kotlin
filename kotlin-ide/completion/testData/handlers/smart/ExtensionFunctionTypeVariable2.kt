fun test(i: Int, foo: Int.(String) -> Char) {
    bar(i.<caret>)
}

fun bar(p1: Char, p2: Int){}

// ELEMENT: foo