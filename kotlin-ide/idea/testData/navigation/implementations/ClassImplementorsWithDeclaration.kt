package testing

open class Test

open class TestOther : <caret>Test()

class TestOtherMore : TestOther()

// REF: (testing).Test
// REF: (testing).TestOther
// REF: (testing).TestOtherMore
