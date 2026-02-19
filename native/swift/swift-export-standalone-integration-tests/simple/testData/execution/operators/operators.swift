import Operators
import Testing


@Test
func testBinaryMathOperators() throws {
    let a = operators.Foo(value: 10), b = operators.Foo(value: 10)

    #expect(a + b === a)
    #expect(a - b === a)
    #expect(a * b === a)
    #expect(a / b === a)
    #expect(a % b === a)

    a += b
    #expect(a == b)
    a -= b
    #expect(a == b)
    a *= b
    #expect(a == b)
    a /= b
    #expect(a == b)
    a %= b
    #expect(a == b)
}

@Test
func testUnaryMathOperators() throws {
    let a = operators.Foo(value: 10)

    #expect(+a === a)
    #expect(-a === a)
    #expect(!a === a)

    #expect(a.inc() === a)
    #expect(a.dec() === a)
}

@Test
func testMiscOperators() throws {
    let a = operators.Foo(value: 10), b = operators.Foo(value: 20)

    #expect(a.contains(other: b))
    #expect(a ~= b)

    #expect(a() === a)
    #expect(a.rangeTo(other: b) === a)
    #expect(a.rangeUntil(other: b) === a)

    #expect(a[42] == 42)
}

@Test
func testComparisonOperators() throws {
    let a = operators.Foo(value: 10), b = operators.Foo(value: 20)

    #expect(!(a < a))
    #expect(a <= a)
    #expect(!(a > a))
    #expect(a >= a)
    #expect(a == a)
    #expect(a != b)

    #expect(a.equals(other: a))
    #expect(!a.equals(other: b))
}