// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

class A<T: Any> {
    operator fun <caret>iterator(): Iterator<T> = throw IllegalStateException("")
}

class B<T: Any> {
    operator fun iterator(): Iterator<T> = throw IllegalStateException("")
}

fun test() {
    for (a in A<String>()) {}
    for (b in B<String>()) {}
    for (a in A<Int>()) {}
    for (b in B<Int>()) {}
}
