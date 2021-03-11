package test

class ExtValPIntInClass<P> {
    var P?.asas: Int
        get() = throw Exception()
        set(p: Int) = throw Exception()
}
