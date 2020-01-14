internal class C1(
        arg1: Int,
        arg2: Int,
        arg3: Int
) {
    constructor(
            x: Int,
            y: Int
    ) : this(x, x + y, 0) {
    }
}

internal class C2(
        private val arg1: Int,
        private val arg2: Int,
        arg3: Int
)