// Abstract
abstract class Base {
    // Redundant final
    final fun foo() {}
    // Abstract
    abstract fun bar()
    // Open
    open val gav = 42
}

class FinalDerived : Base() {
    // Redundant final
    override final fun bar() {}
    // Non-final member in final class
    override open val gav = 13
}
// Open
open class OpenDerived : Base() {
    // Final
    override final fun bar() {}
    // Redundant open
    override open val gav = 13
}
// Redundant final
final class Final
// Interface
interface Interface {
    // Redundant
    abstract fun foo()
    // Redundant
    private final fun bar() {}
    // Redundant
    open val gav: Int
        get() = 42
}
// Derived interface
interface Derived : Interface {
    // Redundant
    override open fun foo() {}
    // Redundant
    final class Nested
}
// Derived abstract class
abstract class AbstractDerived1(override final val gav: Int) : Interface {
    // Redundant
    override open fun foo() {}
}
// Derived abstract class
abstract class AbstractDerived2 : Interface {
    // Final
    override final fun foo() {}
    // Redundant
    override open val gav = 13
}
// Redundant abstract interface
abstract interface AbstractInterface
// Redundant final object
final object FinalObject
// Open interface
open interface OpenInterface
