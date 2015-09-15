internal class C1 internal constructor(arg1: Int,
                                       arg2: Int,
                                       arg3: Int) {

    internal constructor(x: Int,
                         y: Int) : this(x, x + y, 0) {
    }
}

internal class C2 internal constructor(private val arg1: Int,
                                       private val arg2: Int,
                                       arg3: Int)
