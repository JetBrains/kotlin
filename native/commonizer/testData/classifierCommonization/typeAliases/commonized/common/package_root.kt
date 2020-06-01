expect class A()

// Lifted up type aliases:
typealias B = A // class at the RHS
typealias C = B // TA at the RHS

typealias F = List<String> // parameterized type at the RHS
typealias H<T> = List<T> // TA with own parameters

typealias J<T> = Function<T> // function type at the RHS
typealias M = () -> Unit // same return type
typealias O = (String) -> Int // same argument type

typealias Q<T> = (List<M>) -> Map<T, O> // something complex
typealias R = Function<C> // something complex

// Type aliases converted to expect classes:
typealias S = String
expect class T

// Nullability:
typealias U = A // same nullability of the RHS class
expect class V // different nullability of the RHS class
typealias W = U // same nullability of the RHS TA
typealias Y = V // TA at the RHS with the different nullability of own RHS
