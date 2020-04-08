class TestClassName
type TestAlias = TestClassName

val a: Tes<caret>

// Regression for EA-38287 and KT-2758

// EXIST: TestClassName
// ABSENT: TestAlias