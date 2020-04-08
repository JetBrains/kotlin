package to

import a.A

fun A.ext() {
}

var A.p: Int
    get() = 2
    set(i: Int) = throw UnsupportedOperationException()

<caret>