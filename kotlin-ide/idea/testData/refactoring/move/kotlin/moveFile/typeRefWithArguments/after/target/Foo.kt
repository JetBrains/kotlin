package target

class OuterClass<T1> {
    inner class InnerClass<T2>

    class NestedClass<T3>
    typealias NestedType<T> = NestedClass<T>
}
typealias OI<T1, T2> = OuterClass<T1>.InnerClass<T2> // (1)
typealias ON1<T1, T2> = OuterClass<T1>.NestedClass<T2> // (2)
typealias ON2<T1, T2> = OuterClass<T1>.NestedType<T2>