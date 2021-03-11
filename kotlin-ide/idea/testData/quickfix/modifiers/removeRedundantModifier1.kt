// "Remove redundant 'open' modifier" "true"
abstract class B() {
    abstract fun foo()
}

abstract class A() : B() {
    <caret>open abstract override fun foo()
}

/* FIR_COMPARISON */