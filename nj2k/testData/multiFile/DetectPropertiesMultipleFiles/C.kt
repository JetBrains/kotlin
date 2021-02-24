package test

abstract class C internal constructor(override val something1: Int) : B(), I {
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

    fun getFromB5(): String {
        return ""
    }

    override fun setFromB5(value: String?) {
        super.setFromB5(value)
    }
}