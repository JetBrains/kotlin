class Foo(val x: Int) {
    fun <!VIPER_TEXT!>member_fun<!>(): Int {
        return x
    }

    fun <!VIPER_TEXT!>call_member_fun<!>() {
        member_fun()
    }

    fun <!VIPER_TEXT!>sibling_call<!>(other: Foo) {
        other.member_fun()
    }
}

fun <!VIPER_TEXT!>outer_member_fun_call<!>() {
    val f = Foo(3)
    f.member_fun()
}
