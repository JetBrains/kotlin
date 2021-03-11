// MOVE: up
// IS_APPLICABLE: false
// class A
enum class A {
    // U
    U,
    // V
    V;

    // class B
    enum class B {
        // X
        <caret>X,
        // Y
        Y
    }
}