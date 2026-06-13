package test1

open class FirstBase {
    open fun foo(): String = "foo FirstBase"
}

open class Changed : FirstBase()
