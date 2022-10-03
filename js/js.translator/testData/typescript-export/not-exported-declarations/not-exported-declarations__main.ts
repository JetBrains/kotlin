import OnlyFooParamExported = JS_TESTS.foo.OnlyFooParamExported;


function assert(condition: boolean, message: string = "Assertion failed") {
    if (!condition) {
        throw message;
    }
}

function box(): string {
    const onlyFooParamExported = new OnlyFooParamExported("TEST")
    assert(OnlyFooParamExported.length === 1, "Error in constructor'")
    assert(onlyFooParamExported.foo === "TEST", "Error in property 'foo'")

    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(onlyFooParamExported.bar === undefined, "Error in property 'bar'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(onlyFooParamExported.baz === undefined, "Error in property 'baz'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(onlyFooParamExported.inter === undefined, "Error in method 'inter'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(onlyFooParamExported.notExportableReified === undefined, "Error in method 'notExportableReified'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(onlyFooParamExported.notExportableSuspend === undefined, "Error in method 'notExportableSuspend'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(onlyFooParamExported.notExportableReturn === undefined, "Error in method 'notExportableReturn'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(onlyFooParamExported.notExportableExentsionProperty === undefined, "Error in method 'notExportableExentsionProperty'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(OnlyFooParamExported.NotExportableAnnotation === undefined, "Error in nested class 'NotExportableAnnotation'")
    // @ts-expect-error "the param should not be exported either in TS, or in JS"
    assert(OnlyFooParamExported.NotExportableInlineClass === undefined, "Error in nested class 'NotExportableInlineClass'")

    return "OK";
}