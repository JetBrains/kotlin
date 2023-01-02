class ClassB : Interface {
    val x = 3

    override val someValue: Int
        get() = super.someValue + 1
}
