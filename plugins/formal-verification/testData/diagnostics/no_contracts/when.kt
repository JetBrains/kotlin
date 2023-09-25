fun <!VIPER_TEXT!>return_when<!>(a: Boolean, b: Boolean, c: Boolean): Int {
    return when {
        a -> 0
        b -> 1
        c -> 2
        else -> 3
    }
}

fun <!VIPER_TEXT!>when_return<!>(a: Boolean, b: Boolean, c: Boolean): Int {
    when {
        a -> return 0
        b -> return 1
        c -> return 2
        else -> return 3
    }
}

fun <!VIPER_TEXT!>single_branch_when<!>(a: Boolean): Int {
    var x = 1
    when {
        a -> x = 2
    }
    return x
}

fun <!VIPER_TEXT!>no_else_when<!>(a: Boolean, b: Boolean, c: Boolean): Int {
    var y = 0
    when {
        a -> y = 1
        b -> y = 2
        c -> y = 3
    }
    return y
}

fun <!VIPER_TEXT!>when_with_subject_var<!>(x: Int): Int {
    return when (x) {
        1 -> 2
        2 -> 3
        else -> 42
    }
}

fun <!VIPER_TEXT!>when_with_subject_call<!>(x: Int): Int {
    return when (when_with_subject_var(x)) {
        1 -> 2
        2 -> 3
        else -> when (when_with_subject_var(0)){
            3 -> 4
            4 -> 5
            else -> 42
        }
    }
}

fun <!VIPER_TEXT!>empty_when<!>(): Int {
    when { }
    return 1
}

fun <!VIPER_TEXT!>unused_result<!>(): Int {
    val x = when {
        else -> {
            when {
                else -> 5
            }
            0
        }
    }
    return x
}

open class Foo()
class Bar() : Foo()

fun <!VIPER_TEXT!>when_is<!>(x: Foo): Boolean = when(x) {
    is Bar -> true
    else -> false
}

fun <!VIPER_TEXT!>when_subject_val<!>(): Int =
    when (val x = 0) {
        1 -> 1
        else -> x
    }

fun <!VIPER_TEXT!>when_subject_val_nested<!>() {
    when (val x = 1) {
        0 -> 0
        when (val y = 1) {
            1 -> 1
            else -> when (val z = 1) {
                y -> 2
                x+1 -> 3
                else -> 4
            }
        } -> 5
        else -> 6
    }
}

fun <!VIPER_TEXT!>when_subject_var_shadowing<!>() {
    val x = 0
    when (val x = 1) {
        else -> x
    }
}