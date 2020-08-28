// FIR_COMPARISON
class B {
    fun A.extFunInBForA() {}
    fun B.extFunInBForB() {}

}
class A {
    fun B.extFunInAForB() {}
    fun A.extFunInAForA() {}
}

fun A.test() {
    with(B()) {
        <caret>
    }
}

// EXIST: extFunInAForA
// EXIST: extFunInBForA
// EXIST: extFunInAForB
// EXIST: extFunInBForB
