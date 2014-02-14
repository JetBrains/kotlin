package foo

class Foo(val name: String) {

    public override fun toString(): String {
        return "Foo($name)"
    }
}

fun box(): String {
    val a = Foo("James")
    return a.toString()
}