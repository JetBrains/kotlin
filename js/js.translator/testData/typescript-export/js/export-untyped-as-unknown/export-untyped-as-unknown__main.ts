import foo = JS_TESTS.foo;

function assert(condition: boolean, message: string = "Assertion failed") {
    if (!condition) {
        throw message;
    }
}

function box(): string {
    assert(foo._any != null, `_any: non-nullable value expected, got '${foo._any}'`);
    assert(foo._nullable_any == null, `_nullable_any: null expected, got '${foo._nullable_any}'`);
    assert(foo._array_any instanceof Array, `_array_any: array expected, got '${typeof foo._array_any}'`);

    const consumedAny: unknown = foo.consumeAny("value");
    assert(consumedAny === "value", `consumedAny: 'value' expected, got '${consumedAny}'`);

    const consumedNullableAny: unknown = foo.consumeNullableAny(null);
    assert(consumedNullableAny == null, `consumedNullableAny: null expected, got '${consumedNullableAny}'`);

    const produced: unknown = foo.produceDynamic();
    assert(produced === 42, `produced: 42 expected, got '${produced}'`);

    const consumedDynamic: unknown = foo.consumeDynamic("dyn");
    assert(consumedDynamic === "dyn", `consumedDynamic: 'dyn' expected, got '${consumedDynamic}'`);

    const consumedNullableDynamic: unknown = foo.consumeNullableDynamic(null);
    assert(consumedNullableDynamic == null, `consumedNullableDynamic: null expected, got '${consumedNullableDynamic}'`);

    const instance = new foo.WithDynamicMembers();
    const anyProperty: unknown = instance.anyProperty;
    assert(anyProperty != null, `instance.anyProperty: non-nullable value expected, got '${anyProperty}'`);
    const dynamicProperty: unknown = instance.dynamicProperty;
    assert(dynamicProperty === 42, `instance.dynamicProperty: non-nullable value expected, got '${dynamicProperty}'`);
    assert(instance.anyMethod("x") === "x", `instance.anyMethod('x'): 'x' expected, got '${instance.anyMethod("x")}'`);
    assert(instance.dynamicMethod(3) === 3, `instance.dynamicMethod(3): 3 expected, got '${instance.dynamicMethod(3)}'`);

    return "OK";
}
