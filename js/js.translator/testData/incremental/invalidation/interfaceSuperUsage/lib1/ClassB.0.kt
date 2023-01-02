class ClassB : Interface {
    override var someVar: Int?
        get() = super.someVar
        set(value) {
            super.someVar = value
        }

    val x = 1
}
