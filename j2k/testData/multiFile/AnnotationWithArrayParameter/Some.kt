package test

class Some {
    @SomeAnnotation(some = ["Foo"], same = [0])
    fun foo() {

    }

    @SomeAnnotation(some = ["Bar", "Buz"], same = [1, 2])
    fun bar() {

    }
}