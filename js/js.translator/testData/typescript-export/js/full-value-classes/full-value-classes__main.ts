import foo = JS_TESTS.foo;

function assert(condition: boolean, message: string) {
    if (!condition) throw `Assertion failed: ${message}`;
}

async function box(): Promise<string> {
    const direct = new foo.Point(1, 2);
    assert(direct.x === 1, "Point.x");
    assert(direct.y === 2, "Point.y");
    assert(direct.sum() === 3, "Point.sum");
    assert(direct.scaledSum(2) === 6, "Point.@JsName member");
    assert(direct.equals(new foo.Point(1, 2)), "Point.equals");

    const fromSecondary = foo.Point.createFromValue(4);
    assert(fromSecondary.x === 4 && fromSecondary.y === 4, "Point secondary constructor");

    const consumer: foo.PointConsumer = new foo.DefaultPointConsumer();
    assert(consumer.consume(direct) === 3, "exported interface bridge");

    const promisedPoint = await foo.pointAsync(6, 7);
    assert(promisedPoint.x === 6 && promisedPoint.y === 7, "suspend MFVC result");

    const origin = foo.Point.Companion.origin();
    assert(origin.x === 0 && origin.y === 0, "Point companion");

    const created = foo.createPoint(3, 4);
    assert(foo.acceptPoint(created) === 7, "MFVC parameter and result");
    assert(foo.echoNullablePoint(null) == null, "nullable MFVC");
    assert(foo.echoNullablePoint(created)?.y === 4, "nullable MFVC round-trip");

    const points = foo.echoPoints([direct, created]);
    assert(points.length === 2 && points[1].sum() === 7, "MFVC array round-trip");

    const labeled = foo.echoLabeled(new foo.Labeled<number>(42, "answer"));
    assert(labeled.value === 42 && labeled.label === "answer", "generic MFVC round-trip");

    return "OK";
}
