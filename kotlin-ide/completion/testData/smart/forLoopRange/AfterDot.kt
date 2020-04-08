fun foo(p: String) {
    for (i in p.<caret>)
}

fun String.extFun(): Collection<Int>{}

// EXIST: toString
// EXIST: substring
// EXIST: extFun
