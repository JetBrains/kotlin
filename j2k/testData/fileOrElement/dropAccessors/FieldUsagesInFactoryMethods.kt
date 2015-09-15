internal class C internal constructor(internal val arg1: Int) {
    internal var arg2: Int = 0
    internal var arg3: Int = 0

    internal constructor(arg1: Int, arg2: Int, arg3: Int) : this(arg1) {
        this.arg2 = arg2
        this.arg3 = arg3
    }

    internal constructor(arg1: Int, arg2: Int) : this(arg1) {
        this.arg2 = arg2
        arg3 = 0
    }

    init {
        arg2 = 0
        arg3 = 0
    }
}
