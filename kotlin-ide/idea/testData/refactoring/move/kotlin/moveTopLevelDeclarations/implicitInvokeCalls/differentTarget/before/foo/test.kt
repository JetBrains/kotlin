package foo

val valWithFunType = fun (): Unit {}
val valWithExtFunType = fun CrExtended.(): Unit {}
val Int.extValWithFunType get() = fun (): Unit {}
val Int.extValWithExtFunType get() = fun CrExtended.(): Unit {}

class CrExtended

fun <caret>test(ce: CrExtended) {
    valWithFunType()
    ce.valWithExtFunType()
    with(1) {
        extValWithFunType()
        ce.extValWithExtFunType()
    }

    ::valWithFunType
    ::valWithExtFunType
    1::extValWithFunType
    1::extValWithExtFunType
}