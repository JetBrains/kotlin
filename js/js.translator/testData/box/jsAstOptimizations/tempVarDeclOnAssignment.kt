inline fun jsUpper(c: Char) = c.toString().asDynamic().toUpperCase().unsafeCast<String>()

inline fun upper(c: Char): Char = jsUpper(c)[0]

// EXPECT_GENERATED_JS: function=test expect=tempVarDeclOnAssignmentTest.js
fun test(a: Char, b: Char): String {
    return "${upper(a)}${upper(b)}"
}

fun box() = test('o', 'k')
