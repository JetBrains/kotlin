// checks that no special item "ext1 { String, Int -> ... }" created for infix call
infix fun Int.ext1(handler: (String, Int) -> Unit){}
infix fun Int.ext2(c: Char){}

fun foo() {
    val pair = 1 ext<caret>
}

// EXIST: ext1
// EXIST: ext2
// NOTHING_ELSE
