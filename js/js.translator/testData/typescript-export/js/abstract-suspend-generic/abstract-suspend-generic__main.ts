import RemoteValue = JS_TESTS.foo.RemoteValue;
import createRemoteValue = JS_TESTS.foo.createRemoteValue;
import readRemoteValue = JS_TESTS.foo.readRemoteValue;

async function box(): Promise<string> {
    const value: RemoteValue<number> = createRemoteValue();
    const pending: Promise<number> = value.get();
    const first: number = await pending;
    const throughBase: number = await readRemoteValue(value);
    if (first !== 42 || throughBase !== 42) return "wrong generic result";
    return "OK";
}
