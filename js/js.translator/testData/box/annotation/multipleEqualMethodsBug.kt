package foo

annotation class Parent {
    annotation class Child
}

fun box(): String {
    return if (Parent.Child() == Parent.Child()) "OK" else "FAIL"
}
