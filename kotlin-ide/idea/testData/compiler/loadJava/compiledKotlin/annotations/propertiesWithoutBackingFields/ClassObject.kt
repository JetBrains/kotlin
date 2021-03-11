package test

annotation class Anno

class Class {
    companion object {
        @[Anno] val property: Int
            get() = 42
    }
}
