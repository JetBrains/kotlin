// EXPECTED_REACHABLE_NODES: 493
package foo

fun box(): String {

    var x: Char = 'A'
    x++
    assertEquals('B', x)
    ++x
    assertEquals('C', x)

    var y = x++
    assertEquals('C', y)
    assertEquals('D', x)

    y = ++x
    assertEquals('E', y)
    assertEquals('E', x)

    x--
    assertEquals('D', x)
    --x
    assertEquals('C', x)

    return "OK"
}