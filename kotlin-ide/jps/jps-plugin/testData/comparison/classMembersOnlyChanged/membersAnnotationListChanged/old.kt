package test

annotation class Ann1
annotation class Ann2

class A {
    fun annotationListBecameNotEmpty() {}

    @Ann1
    @Ann2
    fun annotationListBecameEmpty() {
    }

    @Ann1
    fun annotationAdded() {
    }

    @Ann1
    @Ann2
    fun annotationRemoved() {
    }

    @Ann1
    fun annotationReplaced() {
    }
}
