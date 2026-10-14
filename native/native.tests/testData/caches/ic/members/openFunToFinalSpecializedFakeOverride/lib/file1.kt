package test

class Builder(val name: String) {
    override fun toString() = name
}

open class Mid<E, B> {
    open fun insert(builder: B, index: Int, element: E): String = "$builder:$index:$element"
}

open class ByteImpl : Mid<Byte, Builder>()

open class LongImpl : Mid<Long, Builder>()

open class RefImpl : Mid<String, Builder>()
