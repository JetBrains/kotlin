// ERROR: Property must be initialized
// ERROR: Property must be initialized
// ERROR: Property must be initialized
// ERROR: Property must be initialized
internal interface I {
    val something1: Int

    val something2: Int

    var something3: Int

    var something4: Int

    var something5: Int

    fun setSomething6(value: Int)
}

internal open class B {
    open val fromB1: String
        get() {
            return ""
        }

    open var fromB2: String
        get() {
            return ""
        }
        set(value: String) {
        }

    open var fromB3: String
        get() {
            return ""
        }
        set(value: String) {
        }

    open var fromB4: String
        get() {
            return ""
        }
        set(value: String) {
        }

    open fun setFromB5(value: String) {
    }
}

internal abstract class C(override val something1: Int) : B(), I {
    private var mySomething6: Int = 0

    override val something2: Int
        get() {
            return 0
        }

    override var something3: Int
        get() {
            return 0
        }
        set(value: Int) {
        }

    override var something4: Int
        get() {
            return 0
        }

    override var something5: Int
        set(value: Int) {

        }

    fun getSomething6(): Int {
        return mySomething6
    }

    override fun setSomething6(value: Int) {
        mySomething6 = value
    }

    override val fromB1: String
        get() {
            return super.fromB1
        }

    override var fromB2: String
        get() {
            return super.fromB2
        }
        set(value: String) {
            super.fromB2 = value
        }

    override var fromB3: String
        get() {
            return super.fromB3
        }

    override var fromB4: String
        set(value: String) {
            super.fromB4 = value
        }

    val fromB5: String
        get() {
            return ""
        }

    override fun setFromB5(value: String) {
        super.setFromB5(value)
    }
}
