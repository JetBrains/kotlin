package test

enum class E : OverrideInEnumEntry {
    X {
        override fun foo() {}
    }
}
