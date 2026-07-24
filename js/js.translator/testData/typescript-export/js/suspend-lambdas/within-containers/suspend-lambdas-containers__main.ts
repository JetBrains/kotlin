import produceListOfSuspendLambdas = JS_TESTS.foo.produceListOfSuspendLambdas;
import reduceListOfSuspendLambdas = JS_TESTS.foo.reduceListOfSuspendLambdas;
import produceMapOfSuspendLambdas = JS_TESTS.foo.produceMapOfSuspendLambdas;
import callMapOfSuspendLambdas = JS_TESTS.foo.callMapOfSuspendLambdas;
import Box = JS_TESTS.foo.Box;
import produceBoxOfSuspendLambda = JS_TESTS.foo.produceBoxOfSuspendLambda;
import callBoxedSuspendLambda = JS_TESTS.foo.callBoxedSuspendLambda;
import KtList = JS_TESTS.kotlin.collections.KtList;
import KtMap = JS_TESTS.kotlin.collections.KtMap;

function assert(condition: boolean, label: string) {
    if (!condition) {
        throw "Assertion failed: " + label;
    }
}

async function box(): Promise<string> {
    const kotlinLambdaBox = produceBoxOfSuspendLambda();
    assert(await kotlinLambdaBox.value() === "BOX", "produceBoxOfSuspendLambda");

    const tsLambdaBox = new Box(async () => "TS_BOX");
    assert(await callBoxedSuspendLambda(tsLambdaBox) === "TS_BOX", "callBoxedSuspendLambda (ts)");

    const kotlinLambdaList = produceListOfSuspendLambdas();
    assert(await reduceListOfSuspendLambdas(kotlinLambdaList, 2) === 20, "reduceListOfSuspendLambdas (kotlin)");

    const tsLambdaList = KtList.fromJsArray([
        async (x: number) => x + 2,
        async (x: number) => x * 5,
    ]);
    assert(await reduceListOfSuspendLambdas(tsLambdaList, 3) === 25, "reduceListOfSuspendLambdas (ts)");

    const kotlinLambdaMap = produceMapOfSuspendLambdas();
    assert(await callMapOfSuspendLambdas(kotlinLambdaMap, "value") === "VALUE", "callMapOfSuspendLambdas (kotlin)");

    const tsLambdaMap = KtMap.fromJsMap(new Map([
        ["answer", async () => "42"],
    ]));
    assert(await callMapOfSuspendLambdas(tsLambdaMap, "answer") === "42", "callMapOfSuspendLambdas (ts)");

    return "OK";
}
