interface C {
    operator fun set(p: String, value: Int)
}

class D(val c: C) {
    fun foo() {
        this.c<caret>[""] = 10
    }
}
