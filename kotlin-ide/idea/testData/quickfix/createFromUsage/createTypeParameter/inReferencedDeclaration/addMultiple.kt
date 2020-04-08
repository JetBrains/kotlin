// "Create type parameters in class 'X'" "true"

class X<T>

fun foo(x: X<<caret>String, Int, Boolean>) {}