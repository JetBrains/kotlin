package test

class ExtValPIntInClass<P> {
    val P.asas: Int
        get() = throw Exception()
}
