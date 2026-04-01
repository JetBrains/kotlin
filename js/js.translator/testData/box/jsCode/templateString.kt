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
    assertEquals(js("'ab'"), js("`a\\\nb`"), "Backslash allows splitting the literal")

    assertEquals(js("'$'"), js("`\\\$`"), "Escape regular dollar sign")
    assertEquals(js("'\${}'"), js("`\\\${}`"), "Escape dollar sign with interpolation")
    assertEquals(js("'aaa'"), js("`\\a\\a\\a`"), "Escape any regular character")
    assertEquals(js(""" '"' """), js(""" `"` """), "Do not escape double qoute")
    assertEquals(js(""" "'" """), js(""" `'` """), "Do not escape single qoute")
    assertEquals(js(""" "\u0000" """), js(""" `\u{0}` """), "Resolve 1-char Unicode codepoint")
    assertEquals(js(""" "A" """), js(""" `\u{41}` """), "Resolve 2-char Unicode codepoint")
    assertEquals(js(""" "\u03A9" """), js(""" `\u{3A9}` """), "Resolve 3-char Unicode codepoint")
    assertEquals(js(""" "\u2764" """), js(""" `\u{2764}` """), "Resolve 4-char Unicode codepoint")
    assertEquals(js(""" "\uD83D\uDCA9" """), js(""" `\u{1F4A9}` """), "Resolve 5-digit Unicode codepoint")
    assertEquals(js(""" "\uDBC0\uDC00" """), js(""" `\u{100000}` """), "Resolve 6-digit Unicode codepoint")
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