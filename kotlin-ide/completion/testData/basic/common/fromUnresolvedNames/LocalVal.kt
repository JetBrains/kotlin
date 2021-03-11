// RUN_HIGHLIGHTING_BEFORE

fun foo(p: Int) {
    print(unresolved0)

    if (p > 0) {
        print(unresolved1)

        val u<caret>

        print(unresolved2)
        if (p > 0) {
            print(unresolved3)
        }

        unresolvedCall()
    }

    print(unresolved4)
}

fun bar() {
    print(unresolvedInBar)
}

// ABSENT: unresolved0
// ABSENT: unresolved1
// EXIST: unresolved2
// EXIST: unresolved3
// ABSENT: unresolved4
// ABSENT: unresolvedInBar
// EXIST: unresolvedCall
