// EXPECTED_REACHABLE_NODES: 1372
package foo

// CHECK_NEW_COUNT: function=box max=10
fun box(): String {
    assertEquals(true, 'A' == 'A')
    assertEquals(false, 'A' != 'A')
    assertEquals(false, 'A' == 'B')
    assertEquals(true, 'A' != 'B')
    assertEquals(false, ('A' as Any) == (65 as Any))
    assertEquals(true, ('A' as Any) != (65 as Any))

    assertEquals(true, 'A' === 'A')
    assertEquals(false, 'A' !== 'A')
    assertEquals(false, 'A' === 'B')
    assertEquals(true, 'A' !== 'B')
    assertEquals(false, ('A' as Any) === (65 as Any))
    assertEquals(true, ('A' as Any) !== (65 as Any))

    assertTrue(bar('Q'))
    assertFalse(bar('W'))

    assertTrue(baz('Q'))
    assertFalse(baz('W'))

    return "OK"
}

fun bar(x: Char) = x.equals('Q')

fun baz(x: Any) = x.equals('Q')
