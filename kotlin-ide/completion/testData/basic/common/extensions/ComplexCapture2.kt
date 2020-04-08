interface D<T>

fun <T1, T2 : D<T1>> T2.ext() {}

class C : D<String> {
    fun foo() {
        this.<caret>
    }
}

// EXIST: ext