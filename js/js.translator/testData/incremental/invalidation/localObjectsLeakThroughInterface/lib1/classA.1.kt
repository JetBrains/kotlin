class ClassA {
    fun leakObject(): Interface {
        val obj = object : Interface {
            override fun getNumber() = 1
        }
        return obj
    }
}
