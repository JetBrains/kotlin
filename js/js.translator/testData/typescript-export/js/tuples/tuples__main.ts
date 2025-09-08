import pair = JS_TESTS.foo.pair;
import Pair = JS_TESTS.kotlin.Pair;
import triple = JS_TESTS.foo.triple;
import Triple = JS_TESTS.kotlin.Triple;
import createPair = JS_TESTS.foo.createPair;
import createTriple = JS_TESTS.foo.createTriple;
import Foo = JS_TESTS.foo.Foo;
import acceptPair = JS_TESTS.foo.acceptPair;
import acceptTriple = JS_TESTS.foo.acceptTriple;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(pair.first === "Test")
    assert(pair.second === 42)
    assert(pair.equals(new Pair("Test", 42)))
    assert(new Pair("Test", 42).equals(pair))
    assert(pair.toString() === '(Test, 42)')

    assert(triple.first === "Test")
    assert(triple.second === 42)
    assert(triple.third === pair)
    assert(triple.equals(new Triple("Test", 42, pair)))
    assert(new Triple("Test", 42, pair).equals(triple))
    assert(triple.toString() === '(Test, 42, (Test, 42))')

    const newPair = createPair()
    assert(newPair.first === 42)
    assert(newPair.second === "Test")
    assert(!newPair.equals(pair))
    assert(newPair.equals(new Pair(42, "Test")))
    assert(new Pair(42, "Test").equals(newPair))
    assert(newPair.toString() == "(42, Test)")

    const newTriple = createTriple()
    assert(newTriple.first instanceof Foo)
    assert(Array.isArray(newTriple.second))
    assert(newTriple.second[0].equals(newPair))
    assert(newTriple.third == "OK")
    assert(!newTriple.equals(triple))
    assert(!newTriple.equals(new Triple(new Foo(), [newPair], "OK")))
    assert(!new Triple(new Foo(), [newPair], "OK").equals(newTriple))
    assert(!newTriple.equals(new Triple(newTriple.first, [newPair], "OK")))
    assert(!new Triple(newTriple.first, [newPair], "OK").equals(newTriple))
    assert(newTriple.toString() == "([object Object], [...], OK)");

    assert(acceptPair(pair) === 42)
    assert(acceptPair(newPair) === "Test")
    assert(acceptPair(new Pair([], globalThis)) === globalThis)

    assert(acceptTriple(triple).equals(new Pair("Test", pair)))
    assert(acceptTriple(newTriple).equals(new Pair(newTriple.first, "OK")))
    assert(acceptTriple(new Triple("OK", 43, globalThis)).equals(new Pair("OK", globalThis)))

    return "OK"
}