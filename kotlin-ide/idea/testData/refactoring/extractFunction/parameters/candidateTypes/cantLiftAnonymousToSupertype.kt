// WITH_RUNTIME

// SIBLING:
val x = object {
    val t = 1

    fun test() {
        <selection>println(this.t)</selection>
    }
}