// KT-8365 Bogus "Receiver never used" warning
class FunctionLike {
    fun invoke() {}
}

class OwnerClass {
    val functionLike = FunctionLike()
    val function = { }
}

val OwnerClass.extFunctionLike = FunctionLike()
val OwnerClass.extFunction = { }


fun OwnerClass.f1() {
    functionLike()
}

fun OwnerClass.f2() {
    function()
}

fun OwnerClass.f3() {
    extFunctionLike()
}

fun OwnerClass.f4() {
    extFunction()
}