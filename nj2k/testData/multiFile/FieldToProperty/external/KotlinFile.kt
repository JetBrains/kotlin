package test

class C : JavaClass() {
    public fun foo(javaClass: JavaClass) {
        javaClass.field++
        myProperty = javaClass.field
        field = myProperty
        field *= 2
    }
}