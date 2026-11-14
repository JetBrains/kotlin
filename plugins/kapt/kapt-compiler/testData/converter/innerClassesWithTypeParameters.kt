// CORRECT_ERROR_TYPES

@file:Suppress("UNRESOLVED_REFERENCE")

class Test {
    private var a = FilterValueDelegate<Float>()
    private inner class FilterValueDelegate<T>
}

class Test2 {
    inner class FilterValueDelegate<T> {
        private var a = Filter2<String>()
        inner class Filter2<X>
    }
}

class Test3 {
    private var a = FilterValueDelegate<Float>()
    private class FilterValueDelegate<T>
}

class GenericOuter<T> {
    inner class Inner<U>

    @JvmField
    val value: GenericOuter<String>.Inner<Missing>? = null
}
