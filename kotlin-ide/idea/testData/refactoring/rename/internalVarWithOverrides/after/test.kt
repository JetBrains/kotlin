sealed class X {
    internal abstract var newOverridableVar: Int

    abstract class Y : X() {
        override var newOverridableVar: Int
            get() = 1
            set(value) {}
    }

    class Z : Y() {
        override var newOverridableVar: Int
            get() = super.newOverridableVar
            set(value) { super.newOverridableVar = value }
    }
}

fun get() : X? {
    return X.Z()
}

fun test() {
    get()?.newOverridableVar
    get()?.newOverridableVar = 3
}
