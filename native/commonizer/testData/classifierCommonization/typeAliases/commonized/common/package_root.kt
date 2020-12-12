expect class A()

// Lifted up type aliases:
typealias B = A // class at the RHS
typealias C = A // TA at the RHS, expanded to the same class
typealias C2 = A // 2x TA at the RHS, expanded to the same class
typealias C3 = A // 3x TA at the RHS, expanded to the same class

typealias D = A // class/TA expanded to the same class at the RHS
typealias E = A // different TAs expanded to the same class at the RHS

typealias F = List<String> // parameterized type at the RHS
typealias H<T> = List<T> // TA with own parameters

typealias I2<T> = List<T>
typealias I3<R> = List<R>
typealias I4 = List<String>

typealias I5<V, K> = Map<K, V>
typealias I6<T, R> = Map<R, T>

typealias I7<K, V> = Map<K, V>
typealias I8<T, R> = Map<R, T>
typealias I9<Q, W> = Map<W, Q>

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
typealias W = A // same nullability of the RHS TA
typealias Y = V // TA at the RHS with the different nullability of own RHS

// Supertypes:
expect class FILE : kotlinx.cinterop.CStructVar

typealias uuid_t = kotlinx.cinterop.CPointer<kotlinx.cinterop.UByteVarOf<kotlinx.cinterop.UByte>>
//                                  ^^^ TODO: ideally, it should be CArrayPointer<UByteVar>
typealias __darwin_uuid_t = kotlinx.cinterop.CArrayPointer<kotlinx.cinterop.UByteVar>

expect val uuid: uuid_t
