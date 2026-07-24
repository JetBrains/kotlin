import sample.Base

class Derived : Base()

fun test(): Int {
    return try {
        Derived().value()
    } catch (e: Error) {
        1
    }
}
