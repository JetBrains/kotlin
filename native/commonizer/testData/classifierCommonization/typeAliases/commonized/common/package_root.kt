expect class A()
// Lifted up type aliases:
typealias B = A // class at the RHS
typealias C = B // TA lifted up as is
typealias C2 = C // TA lifted up as is
typealias C3 = C2 // TA lifted up as is

typealias D = A // class/TA expanded to the same class at the RHS
typealias D2 = A // class/TA expanded to the same class at the RHS
typealias E = B // different TAs use common type from TA-chain
typealias E2 = B // different TAs use common type from TA-chain

typealias F = List<String> // parameterized type at the RHS
typealias H<T> = List<T> // TA with own parameters

typealias I2<T> = List<T>
typealias I3<R> = I2<R>
typealias I4 = I2<String>

typealias I5<V, K> = Map<K, V>
typealias I6<T, R> = I5<T, R>

typealias I7<K, V> = Map<K, V>
typealias I8<T, R> = I7<R, T>
typealias I9<Q, W> = I8<Q, W>

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
expect class X // different nullability of the RHS TA
expect class Y // TA at the RHS with the different nullability of own RHS

// Supertypes:
expect class FILE expect constructor(): kotlinx.cinterop.CStructVar

typealias uuid_t = __darwin_uuid_t
typealias __darwin_uuid_t = kotlinx.cinterop.CArrayPointer<kotlinx.cinterop.UByteVar>

expect val uuid: uuid_t
