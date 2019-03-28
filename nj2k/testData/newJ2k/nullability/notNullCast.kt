// ERROR: Type mismatch: inferred type is Passenger.PassChild? but Passenger.PassChild was expected
// ERROR: Type mismatch: inferred type is Passenger.PassChild? but Passenger.PassChild was expected
class Passenger {
    open class PassParent

    class PassChild : PassParent()

    fun provideNullable(p: Int): PassParent? {
        return if (p > 0) PassChild() else null
    }

    fun test1() {
        val pass = provideNullable(1)!!
        accept1(pass as PassChild)
    }

    fun test2() {
        val pass = provideNullable(1)
        if (1 == 2) {
            assert(pass != null)
            accept2(pass as PassChild?)
        }
        accept2(pass as PassChild?)
    }

    fun accept1(p: PassChild?) {}

    fun accept2(p: PassChild?) {}
}