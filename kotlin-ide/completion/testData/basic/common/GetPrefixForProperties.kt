class C : Thread() {
    val property1: Int = 0
    var property2: Int = 0
    var xxx: Int = 0

    fun getPPP() = 1
}

fun foo(c: C) {
    c.getP<caret>
}

// EXIST: property1
// EXIST: property2
// EXIST: getPPP
// EXIST_JAVA_ONLY: priority
// ABSENT: getPriority
// ABSENT: xxx
