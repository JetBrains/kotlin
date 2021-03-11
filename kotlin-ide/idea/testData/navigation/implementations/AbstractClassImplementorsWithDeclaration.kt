package testing

abstract class Test

open class TestOther : <caret>Test()

class TestOtherMore : TestOther()

// REF: (testing).TestOther
// REF: (testing).TestOtherMore
