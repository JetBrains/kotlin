import WithInternal = JS_TESTS.foo.WithInternal;

function assert(condition: boolean, message: string) {
    if (!condition) {
        throw `FAIL: ${message}`;
    }
}

function box(): string {
    assert(WithInternal.publicVal === "y", "publicVal");
    assert(WithInternal.publicFun() === "y", "publicFun");

    if (false) {
        // @ts-expect-error internal companion block property must not be exported
        WithInternal.secret;
        // @ts-expect-error internal companion block function must not be exported
        WithInternal.secretFun();
        // @ts-expect-error private companion block property must not be exported
        WithInternal.privateVal;
        // @ts-expect-error private companion block function must not be exported
        WithInternal.privateFun();
    }

    return "OK";
}
