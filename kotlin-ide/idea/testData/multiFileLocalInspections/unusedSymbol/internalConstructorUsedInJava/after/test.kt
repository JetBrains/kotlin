class WithInternalConstructor(val x: Int) {
    internal constructor() : this(42)
}