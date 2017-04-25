// MINIFICATION_THRESHOLD: 1433
open class A {
    val a: Int
    open val b: Int
    val c = 99
    val d: Int = 555
        get() = field

    val e: Int
        get() = field

    @get:JsName("getF")
    val f: Int

    @get:JsName("getG")
    val g: Int = 777

    lateinit var h: String

    init {
        foo()
        a = 23
        b = 42
        e = 987
        f = 888
    }
}

fun foo() {}

fun box(): String {
    val aBody = eval("A").toString()
    val expectedRegex = build {
        property("a")
        field("b")
        property("c")
        field("d")
        field("e")
        field("f")
        field("g")
        property("h")
    }
    if (expectedRegex.find(aBody) == null) return "fail"

    return "OK"
}

fun build(f: RegexBuilder.() -> Unit): Regex {
    val builder = RegexBuilder()
    builder.f()
    return Regex(builder.string + "foo()", RegexOption.MULTILINE)
}

class RegexBuilder {
    var string = ""

    fun property(name: String) {
        string += "this.$name\\s+=$ANY_CHARS"
    }

    fun field(name: String) {
        string += "this.$name$FIELD_SUFFIX\\s+=$ANY_CHARS"
    }
}

val ANY_CHARS = "(.|\n)+"
val FIELD_SUFFIX = "_[a-zA-Z0-9\\\$_]+"