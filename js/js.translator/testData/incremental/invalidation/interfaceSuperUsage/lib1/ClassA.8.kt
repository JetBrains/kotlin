class ClassA : Interface {
    override var someVar: Int?
        get() = 1
        set(value) {
            super.someVar = value
        }

    override val someValue: Int
        get() = super.someValue

    override fun someFunction(): Int = super.someFunction()
}
