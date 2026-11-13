import apt.Inspect
import lib.identity.IdentityBase
import lib.value.ValueBase

@Inspect
value class Money(val cents: Long, val currency: String)

@Inspect
abstract value class Shape {
    abstract val area: Int
}

@Inspect
value class Square(val side: Int) : Shape() {
    override val area: Int get() = side * side
}

@Inspect
value class FromValueLibrary(override val x: Int) : ValueBase()

// A Valhalla value class can't extend an identity class, so the value classes extending an identity class are compiled as identity classes
@Inspect
value class FromIdentityLibrary(override val x: Int) : IdentityBase()

@Inspect
abstract value class Middle : IdentityBase()

@Inspect
value class FromMiddle(override val x: Int) : Middle()

@Inspect
class Identity

@Inspect
interface Interface
