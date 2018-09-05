package kotlin


annotation class OptionalExpectationDummy()

// in order to make the compiler ignore semantics of the real OptionalExpectation
typealias OptionalExpectation = OptionalExpectationDummy