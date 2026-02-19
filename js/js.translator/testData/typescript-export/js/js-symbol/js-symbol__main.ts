import SymbolHost = JS_TESTS.foo.SymbolHost;
import SymChild = JS_TESTS.foo.SymChild;
import AbsChild = JS_TESTS.foo.AbsChild;
import IterHost = JS_TESTS.foo.IterHost;
import ktBox = JS_TESTS.foo.ktBox;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(ktBox() === "OK")

    const symbolHost = new SymbolHost();
    assert(symbolHost[Symbol.toPrimitive]("default") === "Converted:default");
    assert(String(symbolHost) === "Converted:string");

    const child = new SymChild();
    assert(child[Symbol.toPrimitive]("X") === "Child:X");
    assert(String(child) === "Child:string");

    const absHost = new AbsChild();
    assert(absHost[Symbol.toPrimitive]("Y") === "Abs:Y");
    assert(String(absHost) === "Abs:string");

    const iterHost = new IterHost();
    const arr = Array.from(iterHost);
    assert(arr.length === 3 && arr[0] === 1 && arr[1] === 2 && arr[2] === 3);

    return "OK";
}
