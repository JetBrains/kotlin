// NEVER_VALIDATE

class Foo(val x: Int) {
    fun <!VIPER_TEXT!>memberFun<!>(): Int {
        return x
    }

    fun <!VIPER_TEXT!>callMemberFun<!>() {
        memberFun()
    }

    fun <!VIPER_TEXT!>siblingCall<!>(other: Foo) {
        other.memberFun()
    }
}

fun <!VIPER_TEXT!>outerMemberFunCall<!>(f: Foo) {
    f.memberFun()
}
