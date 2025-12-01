// FILE: a.kt

inline fun assertRegularTemplates() {
    assertEquals("a == 1", js("`a == \${1}`"), "Simple expression interpolation")
    assertEquals("a == 1", js("`a == \${(function() { return 1; })()}`"), "Complex expression interpolation")

    assertEquals(js("'\\n'"), js("`\\n`"), "Newline character as an escaped escape sequence")
    assertEquals(js("'\\n'"), js("`\n`"), "Newline character as an actual escape sequence")
    assertEquals(js("'\\n'"), js("""`
`"""), "Newline character as an line break in the source code")
    assertEquals(js("'\\t'"), js("`\\t`"), "Tab character as an escape sequence")
    assertEquals(js("'\\t'"), js("`\t`"), "Tab character as an actual tab character")

    assertEquals(js("'$'"), js("`\\\$`"), "Escape regular dollar sign")
    assertEquals(js("'\${}'"), js("`\\\${}`"), "Escape dollar sign with interpolation")
    assertEquals(js("'aaa'"), js("`\\a\\a\\a`"), "Escape any regular character")
    assertEquals(js(""" '"' """), js(""" `"` """), "Do not escape double qoute")
    assertEquals(js(""" "'" """), js(""" `'` """), "Do not escape single qoute")
}

inline fun assertTaggedTemplates() {
    js("""
        let customTag = function (strings) {
            let result = strings[0];
            for (let i = 1; i < arguments.length; i++) {
                result += arguments[i] + '_' + strings[i];
            }
            return result;
        }
    """)
    assertEquals(js("'1_2_3_'"), js("customTag`\${1}\${2}\${3}`"), "Simple template tag")
    assertEquals(js("'1_2_3_'"), js("(customTag)`\${1}\${2}\${3}`"), "Wrapped template tag")
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertRegularTemplates()
    assertTaggedTemplates()

    return "OK"
}