// RUN_HIGHLIGHTING_BEFORE

class C {
    fun foo(p: Int) {
        unresolvedInFoo1()
        if (p > 0) {
            unresolvedInFoo2()
        }
    }

    fun <caret>

    fun bar(s: String) {
        unresolvedInBar()
        s.unresolvedWithReceiver()
    }
}

fun f() {
    unresolvedOutside()
}

// EXIST: unresolvedInFoo1
// EXIST: unresolvedInFoo2
// EXIST: unresolvedInBar
// ABSENT: unresolvedWithReceiver
// ABSENT: unresolvedOutside
