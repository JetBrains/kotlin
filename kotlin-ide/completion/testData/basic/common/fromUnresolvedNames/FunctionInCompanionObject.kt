// RUN_HIGHLIGHTING_BEFORE

class C {
    fun foo(p: Int) {
        unresolvedInFoo1()
        if (p > 0) {
            unresolvedInFoo2()
        }
    }

    companion object {
        fun <caret>

        fun bar() {
            unresolvedInBar()
        }
    }

    fun zoo() {
        unresolvedInZoo()
    }
}

fun f() {
    unresolvedOutside()
}

// EXIST: unresolvedInFoo1
// EXIST: unresolvedInFoo2
// EXIST: unresolvedInBar
// EXIST: unresolvedInZoo
// ABSENT: unresolvedOutside
