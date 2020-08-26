fun method() {
    otherMethod()
    println("Here")
}

fun otherMethod<caret>() {
    return
}
