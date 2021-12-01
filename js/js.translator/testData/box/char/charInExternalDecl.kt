// See KT-43224

external fun actualTypeOfChar(x: Char): String
external fun actualTypeOfMaybeChar(x: Char?): String

external fun getSomeChar(): Char
external fun getMaybeChar(): Char?
external fun getCharNull(): Char?

external val charVal: Char
external val maybeCharVal: Char?
external val charNullVal: Char?

external var charVar: Char
external var maybeCharVar: Char?

external interface I {
    fun foo(): Char
    fun maybeFoo(): Char?
    fun fooNull(): Char?
    fun actualTypeOfChar(x: Char): String
    fun actualTypeOfMaybeChar(x: Char?): String

    val bar: Char
    val maybeBar: Char?
    val barNull: Char?

    var baz: Char
    var maybeBaz: Char?
}

external class A(c: Char): I {
    override fun foo(): Char
    override fun maybeFoo(): Char?
    override fun fooNull(): Char?
    override fun actualTypeOfChar(x: Char): String
    override fun actualTypeOfMaybeChar(x: Char?): String

    override val bar: Char
    override val maybeBar: Char?
    override val barNull: Char?

    override var baz: Char
    override var maybeBaz: Char?
}

val expectedCharRepresentationInProperty = if (testUtils.isLegacyBackend()) "object" else "number"

fun box(): String {

    if (actualTypeOfChar('a') != "number") return "Fail (actualTypeOfChar)"
    if (actualTypeOfMaybeChar('a') != "number") return "Fail (actualTypeOfMaybeChar with nonnull arg)"
    if (actualTypeOfMaybeChar(null) != "object") return "Fail (actualTypeOfMaybeChar with null arg)"

    if (getSomeChar() != 'a') return "Fail (getSomeChar)"
    if (getMaybeChar() != 'a') return "Fail (getMaybeChar)"
    if (getCharNull() != null) return "Fail (getCharNull)"

    if (charVal != 'a') return "Fail (charVal)"
    if (maybeCharVal != 'a') return "Fail (maybeCharVal)"
    if (charNullVal != null) return "Fail (charNullVal)"

    if (charVar != 'a') return "Fail (charVar initial value)"
    charVar = 'b'
    if (js("typeof charVar") != "number") return "Fail (typeof charVar after modification)"
    if (charVar != 'b') return "Fail (charVar after modification)"

    if (maybeCharVar != null) return "Fail (maybeCharVar initial value)"
    maybeCharVar = 'a'
    if (js("typeof maybeCharVar") != "number") return "Fail (typeof maybeCharVar after 1st modification)"
    if (maybeCharVar != 'a') return "Fail (maybeCharVar after 1st modification)"
    maybeCharVar = null
    if (js("typeof maybeCharVar") != "object") return "Fail (typeof maybeCharVar after 2nd modification)"
    if (maybeCharVar != null) return "Fail (maybeCharVar after 2nd modification)"

    val a = A('b')
    if (a.foo() != 'b') return "Fail (a.foo())"
    if (a.maybeFoo() != 'b') return "Fail (a.maybeFoo())"
    if (a.fooNull() != null) return "Fail (a.fooNull())"
    if (a.actualTypeOfChar('a') != "number") return "Fail (a.actualTypeOfChar())"
    if (a.actualTypeOfMaybeChar('a') != "number") return "Fail (a.actualTypeOfMaybeChar() with nonnull arg)"
    if (a.actualTypeOfMaybeChar(null) != "object") return "Fail (a.actualTypeOfMaybeChar() with null arg)"
    if (a.bar != 'b') return "Fail (a.bar)"
    if (a.maybeBar != 'b') return "Fail (a.maybeBar)"
    if (a.barNull != null) return "Fail (a.barNull)"
    if (a.baz != 'q') return "Fail (a.baz initial value)"
    a.baz = 'r'
    if (js("typeof a._baz") != expectedCharRepresentationInProperty) return "Fail (typeof a._baz after modification)"
    if (a.baz != 'r') return "Fail (a.baz after modification)"
    if (a.maybeBaz != null) return "Fail (a.maybeBaz initial value)"
    a.maybeBaz = 's'
    if (js("typeof a._nullableBaz") != expectedCharRepresentationInProperty) return "Fail (typeof a._nullableBaz after 1st modification)"
    if (a.maybeBaz != 's') return "Fail (a.maybeBaz after 1st modification)"
    a.maybeBaz = null
    if (js("typeof a._nullableBaz") != "object") return "Fail (typeof a._nullableBaz after 2nd modification)"
    if (a.maybeBaz != null) return "Fail (a.maybeBaz after 2nd modification)"

    val b: I = A('e')
    if (b.foo() != 'e') return "Fail (b.foo())"
    if (b.maybeFoo() != 'e') return "Fail (b.maybeFoo())"
    if (b.fooNull() != null) return "Fail (b.fooNull())"
    if (b.actualTypeOfChar('a') != "number") return "Fail (b.actualTypeOfChar())"
    if (b.actualTypeOfMaybeChar('a') != "number") return "Fail (b.actualTypeOfMaybeChar() with nonnull arg)"
    if (b.actualTypeOfMaybeChar(null) != "object") return "Fail (b.actualTypeOfMaybeChar() with null arg)"
    if (b.bar != 'e') return "Fail (b.bar)"
    if (b.maybeBar != 'e') return "Fail (b.maybeBar)"
    if (b.barNull != null) return "Fail (b.barNull)"
    if (b.baz != 'q') return "Fail (b.baz initial value)"
    b.baz = 'r'
    if (js("typeof b._baz") != expectedCharRepresentationInProperty) return "Fail (typeof b._baz after modification)"
    if (b.baz != 'r') return "Fail (b.baz after modification)"
    if (b.maybeBaz != null) return "Fail (b.maybeBaz initial value)"
    b.maybeBaz = 's'
    if (js("typeof b._nullableBaz") != expectedCharRepresentationInProperty) return "Fail (typeof b._nullableBaz after 1st modification)"
    if (b.maybeBaz != 's') return "Fail (b.maybeBaz after 1st modification)"
    b.maybeBaz = null
    if (js("typeof b._nullableBaz") != "object") return "Fail (typeof b._nullableBaz after 2nd modification)"
    if (b.maybeBaz != null) return "Fail (b.maybeBaz after 2nd modification)"

    return "OK"
}
