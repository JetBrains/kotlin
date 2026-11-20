import Base = JS_TESTS.foo.Base;
import Implementation = JS_TESTS.foo.Implementation;

async function box(): Promise<string> {
    const value: Base = new Implementation();
    if (await value.value(2) !== "K2") return "explicit argument";
    if (await value.value() !== "K7") return "omitted argument";
    return "OK";
}
