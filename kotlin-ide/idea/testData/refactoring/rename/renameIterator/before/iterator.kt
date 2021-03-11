class A {
    public operator fun iterator(): Iterator<String> = throw IllegalStateException("")
}

fun test() {
    for (a in A<String>()) {}
    a.iterator()
}