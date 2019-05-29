internal interface I {
    val something1: Int
    val something2: Int
    var something3: Int
    fun getSomething4(): Int
    fun setSomething4(value: Int)
    fun getSomething5(): Int
    fun setSomething5(value: Int)
    fun setSomething6(value: Int)
}

internal interface I1 : I {
    fun setSomething1(value: Int)
    val something6: Int
}

internal open class B {
    open val fromB1: String
        get() = ""

    open var fromB2: String
        get() = ""
        set(value) {}

    open var fromB3: String
        get() = ""
        set(value) {}

    open var fromB4: String
        get() = ""
        set(value) {}

    open fun setFromB5(value: String) {}
}

internal abstract class C(override val something1: Int) : B(), I {
    private var mySomething6 = 0

    override val something2: Int
        get() = 0

    override var something3: Int
        get() = 0
        set(value) {}

    override fun getSomething4(): Int {
        return 0
    }

    override fun setSomething5(value: Int) {}
    fun getSomething6(): Int {
        return mySomething6
    }

    override fun setSomething6(value: Int) {
        mySomething6 = value
    }

    override val fromB1: String
        get() = super.fromB1

    override var fromB2: String
        get() = super.fromB2
        set(value) {
            super.fromB2 = value
        }

    override var fromB3: String
        get() = super.fromB3
        set(fromB3) {
            super.fromB3 = fromB3
        }

    override var fromB4: String
        get() = super.fromB4
        set(value) {
            super.fromB4 = value
        }

    fun getFromB5(): String {
        return ""
    }

    override fun setFromB5(value: String) {
        super.setFromB5(value)
    }
}