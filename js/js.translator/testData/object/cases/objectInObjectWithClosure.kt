package foo

class Foo {
    fun bar(param: String): String {
        val local = "world!"
        var a = object {
            val b = object {
                val bar = param + local
            }
        }
        return a.b.bar
    }
}

fun box(): Boolean {
    return Foo().bar("hello ") == "hello world!"
}

