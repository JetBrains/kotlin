package test

interface Interface {
    fun foo(): Int
}

class InterfaceImpl : Interface {
    override fun foo(): Int = 0
}

fun getInterface(): Interface =
    InterfaceImpl()