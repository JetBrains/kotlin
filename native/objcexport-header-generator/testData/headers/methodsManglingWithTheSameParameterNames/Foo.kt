interface Foo1Params {
    fun bar(param1: String)
    fun bar(param1: Int)
    fun bar(param1: Boolean)
}

interface Foo2Params {
    fun bar(param1: String, param2: Int)
    fun bar(param1: Int, param2: String)
    fun bar(param1: Boolean, param2: Int)
}

interface Foo3Params {
    fun bar(param1: String, param2: Int, param3: Boolean)
    fun bar(param1: Boolean, param2: String, param3: Int)
    fun bar(param1: Int, param2: Boolean, param3: String)
}