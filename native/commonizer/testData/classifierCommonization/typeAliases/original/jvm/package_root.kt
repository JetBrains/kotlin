class A

// Lifted up type aliases:
typealias B = A // class at the RHS
typealias C = B // TA at the RHS

typealias D = A // class/TA at the RHS
typealias E = B // different TAs at the RHS

typealias F = List<String> // parameterized type at the RHS
typealias G = List<String> // different parameterized types at the RHS

typealias H<T> = List<T> // TA with own parameters
typealias I<R> = List<R> // TAs with own parameters with different names

typealias J<T> = Function<T> // function type at the RHS
typealias K<R> = Function<R> // function types with different type parameter names
typealias L<T> = Function<T> // different kinds of function types
typealias M = () -> Unit // same return type
typealias N = () -> Unit // different return types
typealias O = (String) -> Int // same argument type
typealias P = (String) -> Int // different argument types

typealias Q<T> = (List<M>) -> Map<T, O> // something complex
typealias R = Function<C> // something complex

// Type aliases converted to expect classes:
typealias S = String
typealias T = String

// Nullability:
typealias U = A // same nullability of the RHS class
typealias V = A // different nullability of the RHS class
typealias W = U // same nullability of the RHS TA
typealias X = U // different nullability of the RHS TA
typealias Y = V // TA at the RHS with the different nullability of own RHS
