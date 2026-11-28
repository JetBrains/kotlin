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

    const single = new foo.Single(5);
    assert(single.x === 5, "Single.x");
    assert(single.equals(new foo.Single(5)), "Single.equals");
    assert(foo.acceptSingle(foo.createSingle(6)) === 6, "single-field full value class parameter and result");
    assert(foo.echoNullableSingle(null) == null, "nullable single-field full value class");
    assert(foo.echoNullableSingle(single)?.x === 5, "nullable single-field full value class round-trip");
    const singles = foo.echoSingles([single, foo.createSingle(6)]);
    assert(singles.length === 2 && singles[1].x === 6, "single-field full value class array round-trip");

    const inlineSingle = new foo.InlineSingle(7);
    assert(inlineSingle.x === 7, "InlineSingle.x");
    assert(inlineSingle.equals(new foo.InlineSingle(7)), "InlineSingle.equals");
    assert(foo.acceptInlineSingle(foo.createInlineSingle(8)) === 8, "inline value class parameter and result");
    assert(foo.echoNullableInlineSingle(null) == null, "nullable inline value class");
    assert(foo.echoNullableInlineSingle(inlineSingle)?.x === 7, "nullable inline value class round-trip");
    const inlineSingles = foo.echoInlineSingles([inlineSingle, foo.createInlineSingle(8)]);
    assert(inlineSingles.length === 2 && inlineSingles[1].x === 8, "inline value class array round-trip");

    return "OK";
}
