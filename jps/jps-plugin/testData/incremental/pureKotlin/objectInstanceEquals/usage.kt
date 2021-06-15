package test

fun provideSea(): Sea = Red

fun takeBoolean(b: Boolean) {}

fun test() {
    // Must be recompiled if any of Sea subclasses is changed.
    takeBoolean(Red == provideSea())
}
