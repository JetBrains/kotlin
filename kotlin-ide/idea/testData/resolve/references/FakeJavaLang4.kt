package nonRoot

fun foo() {
    java.lang.<caret>Fake() // qualification doesn't help, because we are in other package
}

//REF_EMPTY