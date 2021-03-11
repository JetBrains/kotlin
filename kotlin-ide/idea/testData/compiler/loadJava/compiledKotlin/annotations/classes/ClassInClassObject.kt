package test

annotation class Anno

class Class {
    companion object {
        @Anno class Nested
    }
}
