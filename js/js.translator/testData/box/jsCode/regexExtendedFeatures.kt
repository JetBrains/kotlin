fun box(): String {

    assertEquals(true, js("/^.$/u.test('🚀')"))
    assertEquals(true, js("(() => { const pattern = /a/y; pattern.lastIndex = 1; return pattern.test('ba'); })()"))
    assertEquals(true, js("/a.b/s.test('a\\nb')"))
    assertEquals(true, js("/^\\p{Letter}+$/u.test('Hello')"))
    assertEquals("42", js("(/(?<value>\\d+)/.exec('42').groups.value)"))
    assertEquals("42", js("(/(?<=\\$)\\d+/.exec('$42')[0])"))
    assertEquals(
        true,
        js(
            """(() => { 
                 const match = /(ab)/d.exec('zabz');
                 return match.indices[0][0] === 1 
                        && match.indices[0][1] === 3 
                        && match.indices[1][0] === 1 
                        && match.indices[1][1] === 3;
            })()"""
        )
    )

    return "OK"
}
