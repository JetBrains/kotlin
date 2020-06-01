actual class A actual constructor()

// Lifted up type aliases:
typealias D = A // class/TA at the RHS
typealias E = B // different TAs at the RHS

typealias G = List<String> // different parameterized types at the RHS

typealias I<R> = List<R> // TAs with own parameters with different names

typealias K<R> = Function<R> // function types with different type parameter names
typealias L<T> = Function<T> // different kinds of function types
typealias N = () -> Unit // different return types
typealias P = (String) -> Int // different argument types

// Type aliases converted to expect classes:
actual typealias T = String

// Nullability:
actual typealias V = A // different nullability of the RHS class
typealias X = U // different nullability of the RHS TA
