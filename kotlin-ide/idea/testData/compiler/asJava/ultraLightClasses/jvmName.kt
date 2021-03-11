class C {
    var rwProp: Int
        @JvmName("get_rwProp")
        get() = 0
        @JvmName("set_rwProp")
        set(v) {}

    @get:JvmName("xyz1")
    @set:JvmName("xyz2")
    var xyz: String
        get() = ""
        set(value) {}

    @get:JvmName("hasBigArity")
    val hasBigArity: Boolean
        get() = true

    fun getRwProp(): Int = 123
    fun setRwProp(v: Int) {}

    fun foo(x: List<String>) {}
    @JvmName("fooInt")
    fun foo(x: List<Int>) {}
}
