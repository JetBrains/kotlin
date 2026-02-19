import producer = JS_TESTS.foo.producer;
import consumer = JS_TESTS.foo.consumer;
import A = JS_TESTS.foo.A;
import B = JS_TESTS.foo.B;
import childProducer = JS_TESTS.foo.childProducer;
import childConsumer = JS_TESTS.foo.childConsumer;
import genericChildProducer = JS_TESTS.foo.genericChildProducer;
import genericChildConsumer = JS_TESTS.foo.genericChildConsumer;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    const nonExportedType = producer(42)
    const a = new A(nonExportedType)
    const b = new B(43)

    assert(consumer(nonExportedType) == 42)

    a.value = producer(24)
    assert(consumer(b) == 43)
    assert(consumer(a.value) == 24)
    assert(consumer(a.increment(nonExportedType)) == 43)

    const oneMoreNonExportedType = childProducer(322)
    assert(consumer(oneMoreNonExportedType) == 322)
    assert(childConsumer(oneMoreNonExportedType) == 322)

    return "OK";
}