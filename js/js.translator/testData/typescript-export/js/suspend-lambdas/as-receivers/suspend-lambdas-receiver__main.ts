import receiverSuspendLambda = JS_TESTS.foo.receiverSuspendLambda;
import callReceiverSuspendLambda = JS_TESTS.foo.callReceiverSuspendLambda;
import nullableReceiverSuspendLambda = JS_TESTS.foo.nullableReceiverSuspendLambda;
import callNullableReceiverLambda = JS_TESTS.foo.callNullableReceiverLambda;
import Receiver = JS_TESTS.foo.Receiver;
import classReceiverSuspendLambda = JS_TESTS.foo.classReceiverSuspendLambda;
import callClassReceiverLambda = JS_TESTS.foo.callClassReceiverLambda;

function assert(condition: boolean, label: string) {
    if (!condition) {
        throw "Assertion failed: " + label;
    }
}

async function box(): Promise<string> {
    assert(await receiverSuspendLambda("R", 7) === "R7", "receiverSuspendLambda");
    assert(
        await callReceiverSuspendLambda(async (_this_: string, x: number) => _this_ + ":" + x, "TS", 8) === "TS:8",
        "callReceiverSuspendLambda"
    );

    assert(
        await nullableReceiverSuspendLambda("hi") === "hi",
        "nullableReceiverSuspendLambda (non-null)"
    );
    assert(
        await nullableReceiverSuspendLambda(null) === "null-receiver",
        "nullableReceiverSuspendLambda (null)"
    );
    assert(
        await callNullableReceiverLambda(
            async (_this_) => _this_ == null ? "ts-null" : "ts:" + _this_,
            null
        ) === "ts-null",
        "callNullableReceiverLambda (TS, null)"
    );
    assert(
        await callNullableReceiverLambda(
            async (_this_) => _this_ == null ? "ts-null" : "ts:" + _this_,
            "world"
        ) === "ts:world",
        "callNullableReceiverLambda (TS, non-null)"
    );

    const recv = new Receiver(5);
    assert(await classReceiverSuspendLambda(recv) === 10, "classReceiverSuspendLambda (Kotlin)");
    assert(
        await callClassReceiverLambda(async (_this_: Receiver) => _this_.v + 100, recv) === 105,
        "callClassReceiverLambda (TS)"
    );

    return "OK";
}
