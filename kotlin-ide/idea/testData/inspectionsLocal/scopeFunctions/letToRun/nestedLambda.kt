// WITH_RUNTIME

val x = hashSetOf("abc").<caret>let {
    it.forEach {
        println(it)
    }
}
