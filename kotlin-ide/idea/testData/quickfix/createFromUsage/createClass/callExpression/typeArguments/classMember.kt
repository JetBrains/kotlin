// "Create class 'Foo'" "true"

class B<T>(val t: T) {

}

class A<T>(val b: B<T>) {
    fun test() = B.<caret>Foo<Int, String>(2, "2")
}