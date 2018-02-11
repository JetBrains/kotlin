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