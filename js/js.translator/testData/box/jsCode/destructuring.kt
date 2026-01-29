// FILE: a.kt

inline fun checkObjectPatterns() {
    assertEquals(1, js("(function () { var { a } = { a: 1 }; return a; })()"))
    assertEquals(1, js("(function () { let { a } = { a: 1 }; return a; })()"))
    assertEquals(1, js("(function () { const { a } = { a: 1 }; return a; })()"))
    assertEquals(1, js("(function () { let a = 15; ({ a } = { a: 1 }); return a; })()"))
    assertEquals(1, js("(function () { const { a: d } = { a: 1, b: 2, c: 3 }; return d; })()"))
    assertEquals(1, js("(function () { const { a: { b: c } } = { a: { b: 1 } }; return c; })()"))
    assertEquals(2, js("(function () { const { b = 2 } = { a: 1 }; return b; })()"))
    assertEquals(2, js("(function () { const { b: c = 2 } = { a: 1 }; return c; })()"))
    assertEquals(1, js("(function () { const { a: { c: d = 1 } = {} } = { a: { b: 1 } }; return d; })()"))
    assertEquals(1, js("(function () { const {} = { a: 1 }; return 1; })()"))
    assertEquals(1, js("(function () { const { [('a')]: b = 12 } = { a: 1 }; return b; })()"))
    assertEquals(1, js("(function () { const { [('x', 'y', 'a')]: b = 12 } = { a: 1 }; return b; })()"))
    assertEquals(6, js("(function () { const { a: [b, c, d] } = { a: [1, 2, 3] }; return b + c + d; })()"))
    assertEquals(3, js("(function ({ a, b }) { return a + b; })({ a: 1, b: 2 })"))
    assertEquals(4, js("(function () { return (({ a, b }) => a + b)({ a: 1, b: 3 }); })()"))
    assertEquals(6, js("(function () { let sum = 0; for (const { a } of [{ a: 1 }, { a: 2 }, { a: 3 }]) { sum += a; } return sum; })()"))
    assertEquals(5, js("(function () { try { throw { error: 5 }; } catch ({ error }) { return error; } })()"))
}

inline fun checkArrayPatterns() {
    assertEquals(1, js("(function () { var [a] = [1]; return a; })()"))
    assertEquals(1, js("(function () { let [a] = [1]; return a; })()"))
    assertEquals(1, js("(function () { const [a] = [1]; return a; })()"))
    assertEquals(1, js("(function () { let a = 15; ([a] = [1]); return a; })()"))
    assertEquals(6, js("(function () { const [a, b, c] = [1, 2, 3]; return a + b + c; })()"))
    assertEquals(3, js("(function () { const [, , c] = [1, 2, 3]; return c; })()"))
    assertEquals(2, js("(function () { const [a = 2] = []; return a; })()"))
    assertEquals(1, js("(function () { const [a = 2] = [1]; return a; })()"))
    assertEquals(1, js("(function () { const [[a]] = [[1]]; return a; })()"))
    assertEquals(6, js("(function () { const [[a, b], c] = [[1, 2], 3]; return a + b + c; })()"))
    assertEquals(1, js("(function () { const [] = [1]; return 1; })()"))
    assertEquals(3, js("(function () { const [a, ...b] = [1, 2, 3]; return a + b.length; })()"))
    assertEquals(6, js("(function () { const [{ a }, [b, c]] = [{ a: 1 }, [2, 3]]; return a + b + c; })()"))
    assertEquals(3, js("(function ([a, b]) { return a + b; })([1, 2])"))
    assertEquals(4, js("(function () { return (([a, b]) => a + b)([1, 3]); })()"))
    assertEquals(6, js("(function () { let sum = 0; for (const [a] of [[1], [2], [3]]) { sum += a; } return sum; })()"))
    assertEquals(7, js("(function () { try { throw [7]; } catch ([error]) { return error; } })()"))
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    checkObjectPatterns()
    checkArrayPatterns()
    return "OK"
}