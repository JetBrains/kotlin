// PROBLEM: none

inline class InlineClass(val x: Int) {
    <caret>constructor() : this(42)
}

val call = InlineClass()