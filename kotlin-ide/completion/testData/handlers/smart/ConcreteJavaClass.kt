open class A<T : Any>(val javaClass: Class<T>?)

class B : A<java.io.File>(<caret>)

// ELEMENT: File
