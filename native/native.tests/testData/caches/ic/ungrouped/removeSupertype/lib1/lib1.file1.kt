package test1

open class Base {
    open fun foo(): String = "foo Base"
}

open class Changed : Base()
