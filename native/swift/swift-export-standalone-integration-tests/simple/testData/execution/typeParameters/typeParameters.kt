// KIND: STANDALONE
// MODULE: TypeParameters
// FILE: main.kt

class Foo<T>(val genericTypeParam: T) {
    fun getGenericTypeParam(): T {
        return genericTypeParam
    }
}