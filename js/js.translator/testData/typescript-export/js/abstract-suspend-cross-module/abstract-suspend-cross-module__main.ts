import createValue = main.foo.createValue;
import readValue = main.foo.readValue;

async function box(): Promise<string> {
    const value = createValue();
    if (await value.value() !== "OK!") return "omitted argument";
    if (await value.value("?") !== "OK?") return "explicit argument";
    if (await readValue(value) !== "OK!") return "Kotlin base dispatch";
    return "OK";
}
