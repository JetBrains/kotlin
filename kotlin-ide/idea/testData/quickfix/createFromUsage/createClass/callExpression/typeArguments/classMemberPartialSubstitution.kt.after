// "Create class 'Foo'" "true"

class B<T>(val t: T) {
    class Foo<U>(i: Int, u: U) {

    }

}

class A<T>(val b: B<T>) {
    fun test() = B.Foo<String>(2, "2")
}