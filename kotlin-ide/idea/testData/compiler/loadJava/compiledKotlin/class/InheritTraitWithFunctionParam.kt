package test

interface Trait {
    open fun f(a: String) {
    }
}

open class Class : Trait {
}