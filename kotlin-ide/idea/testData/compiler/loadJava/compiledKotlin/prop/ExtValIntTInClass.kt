package test

class ExtValInClass<T> {
    val Int.asas: T
        get() = throw Exception()
}
