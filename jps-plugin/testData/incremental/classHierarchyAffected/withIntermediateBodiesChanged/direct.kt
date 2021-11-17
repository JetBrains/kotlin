package foo

open class DirectBase {
    open val x get() = 0
}

class DirectChild : DirectInter()