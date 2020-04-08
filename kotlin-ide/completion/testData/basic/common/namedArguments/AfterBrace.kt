fun small(param: Int) {
}

fun test() = small(<caret>)

// EXIST: {"lookupString":"param =","tailText":" Int","itemText":"param ="}