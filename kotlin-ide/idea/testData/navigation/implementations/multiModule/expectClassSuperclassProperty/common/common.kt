package test

open class SimpleParent {
    open val <caret>bar: Int get() = 1
}

expect open class ExpectedChild : SimpleParent {
    override val bar: Int
}

class ExpectedChildChild : ExpectedChild() {
    override val bar: Int get() = 1
}

class SimpleChild : SimpleParent() {
    override val bar: Int get() = 1
}

// DISTINCT_REF
// REF: [testModule_Common] (in test.ExpectedChild).bar
// REF: [testModule_Common] (in test.ExpectedChildChild).bar
// REF: [testModule_Common] (in test.SimpleChild).bar
// REF: [testModule_JVM] (in test.ExpectedChild).bar
// REF: [testModule_JVM] (in test.ExpectedChildChildJvm).bar