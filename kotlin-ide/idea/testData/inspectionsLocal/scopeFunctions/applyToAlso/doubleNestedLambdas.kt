// WITH_RUNTIME

val x = hashSetOf("abc").<caret>apply {
    forEach {
        forEach {
            println(this)
        }
    }
}
