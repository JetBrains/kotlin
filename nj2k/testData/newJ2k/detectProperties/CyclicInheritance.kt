// ERROR: There's a cycle in the inheritance hierarchy for this type
// ERROR: There's a cycle in the inheritance hierarchy for this type
internal interface I {
    val isSomething1: Boolean
    val isSomething2: Boolean?
    val isSomething3: Int
    fun isSomething4(): Boolean
    fun setSomething4(value: Boolean)
    fun isSomething5(): Boolean
    fun setSomething5(value: Boolean)
    fun getSomething6(): Boolean
    fun setSomething6(value: Boolean)
}

internal abstract class A : B(), I {
    override var isSomething1: Boolean
        get() = true
        set(b) {}

    override fun isSomething4(): Boolean {
        return false
    }

    override fun setSomething5(value: Boolean) {}
    override fun setSomething6(value: Boolean) {}
}

internal abstract class B : A(), I {
    override var isSomething1: Boolean
        get() = true
        set(b) {}

    override fun isSomething4(): Boolean {
        return false
    }

    override fun setSomething5(value: Boolean) {}
    override fun setSomething6(value: Boolean) {}
}