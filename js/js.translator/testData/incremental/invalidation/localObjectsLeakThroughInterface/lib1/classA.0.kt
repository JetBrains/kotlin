class ClassA {
    fun leakObject(): Interface {
        val obj = object : Interface {}
        return obj
    }
}
