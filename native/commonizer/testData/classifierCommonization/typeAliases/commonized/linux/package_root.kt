actual class A actual constructor()

// Lifted up type aliases:
typealias G = List<Int> // different parameterized types at the RHS

typealias I<T> = List<T> // TAs with own parameters with different names

typealias K<T> = Function<T> // function types with different type parameter names
typealias L<T> = () -> T // different kinds of function types
typealias N = () -> Any // different return types
typealias P = (Int) -> Int // different argument types

// Type aliases converted to expect classes:
actual typealias T = Int

// Nullability:
actual typealias V = A? // different nullability of the RHS class
typealias X = U? // different nullability of the RHS TA

// Supertypes:
actual typealias FILE = _IO_FILE
final class _IO_FILE : kotlinx.cinterop.CStructVar {}

actual val uuid: uuid_t get() = TODO()
