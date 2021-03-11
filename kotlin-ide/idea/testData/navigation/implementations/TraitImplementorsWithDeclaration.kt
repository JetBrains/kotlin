package testing

interface Test

interface TestMore: <caret>Test

open class TestClass1: Test

class TestClass2: TestClass1()

class TestClass3: TestMore

// REF: (testing).TestMore
// REF: (testing).TestClass1
// REF: (testing).TestClass2
// REF: (testing).TestClass3
