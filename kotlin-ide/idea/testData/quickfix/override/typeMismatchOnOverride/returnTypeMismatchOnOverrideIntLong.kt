// "Change return type to 'Int'" "true"
abstract class A {
    abstract fun foo() : Int;
}

abstract class B : A() {
    abstract override fun foo(): Long<caret>
}

/* FIR_COMPARISON */