package test1

open class FirstBase {
    open fun foo(): String = "foo FirstBase"
}

open class Changed : SecondBase()

open class SecondBase {
    open fun foo(): String = "foo SecondBase"
}
