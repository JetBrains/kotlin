import Base = JS_TESTS.foo.Base;
import anonymous = JS_TESTS.foo.anonymous;
import named = JS_TESTS.foo.named;
import callBase = JS_TESTS.foo.callBase;
import RemoteValue = JS_TESTS.foo.RemoteValue;
import createRemoteValue = JS_TESTS.foo.createRemoteValue;
import readRemoteValue = JS_TESTS.foo.readRemoteValue;

async function testAnonymousAndNamed(): Promise<string> {
    const anonymousValue: Base = anonymous();
    const namedValue: Base = named();
    const results = [
        await anonymousValue.value(42),
        await namedValue.value(42),
        await callBase(anonymousValue),
        await callBase(namedValue)
    ].join(";");
    return results === "OK42;OK42;OK42;OK42" ? "OK" : results;
}

async function testGeneric(): Promise<string> {
    const value: RemoteValue<number> = createRemoteValue();
    const pending: Promise<number> = value.get();
    const first: number = await pending;
    const throughBase: number = await readRemoteValue(value);
    if (first !== 42 || throughBase !== 42) return "wrong generic result";
    return "OK";
}

async function testClassAndInterface(): Promise<string> {
    const lib = JS_TESTS.foo;
    const value = new lib.CombinedImplementation();
    const direct = await value.value(1);
    const viaBase = await lib.callCombinedBase(value, 2);
    const viaInterface = await lib.callCombinedInterface(value, 3);
    const result = direct + ";" + viaBase + ";" + viaInterface;
    return result === "OK1;OK2;OK3" ? "OK" : result;
}

async function box(): Promise<string> {
    const results = [
        await testAnonymousAndNamed(),
        await testGeneric(),
        await testClassAndInterface()
    ];
    return results.every(result => result === "OK") ? "OK" : results.join(";");
}
