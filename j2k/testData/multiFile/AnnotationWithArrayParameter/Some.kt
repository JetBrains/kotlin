package test

class Some {
    @SomeAnnotation(some = arrayOf("Foo"), same = intArrayOf(0))
    fun foo() {

    }

    @SomeAnnotation(some = arrayOf("Bar", "Buz"), same = intArrayOf(1, 2))
    fun bar() {

    }
}