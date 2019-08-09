package lombok

annotation class Data
data class Pojo(val foo: Int = 0, val bar: Long = 0, val baz: String? = null) 