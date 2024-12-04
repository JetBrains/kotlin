class ClassA : Interface {
    override var someVar: Int?
        get() = super.someVar
        set(value) {
            super.someVar = value
        }
}
