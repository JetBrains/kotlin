// WITH_RUNTIME

annotation class AllOpen

annotation class Plain(val name: String, val index: Int) {
    companion object {
        @JvmStatic val staticProperty = 42
        @JvmStatic fun staticFun() {}
    }
}

@AllOpen
annotation class MyComponent(val name: String, val index: Int) {
    companion object {
        @JvmStatic val staticProperty = 42
        @JvmStatic fun staticFun() {}
    }
}
