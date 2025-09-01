// FIR_IDENTICAL
annotation class NoArg

@NoArg
class MyClass(val s: String)

fun usage() {
    MyClass<!NO_VALUE_FOR_PARAMETER!>()<!>
}
