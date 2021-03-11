package test

public open class MethodWithTypePRefClassP<P>() {
    public fun <Q : P> f() : Unit = Unit
}
