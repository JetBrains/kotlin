
@DslMarker
annotation class DSL

@DSL
class AAA {
    fun sub(l: BBB.() -> Unit) {
        l(BBB())
    }
}

@DSL
class BBB

object AExtSpace {
    fun AAA.aaa() {

    }
}

object BExtSpace {
    fun BBB.aaa() {

    }
}