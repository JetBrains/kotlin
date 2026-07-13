import {
    callIntLambda,
    reuseLibSuspendLambda,
} from "./suspend-lambdas-cross-module-lib_v5.mjs";

function assert(condition: boolean, label: string) {
    if (!condition) {
        throw "Assertion failed: " + label;
    }
}

export async function box(): Promise<string> {
    assert(await reuseLibSuspendLambda()(38) === 42, "reuseLibSuspendLambda");
    assert(await callIntLambda(async (x: number) => x * 2, 21) === 42, "callIntLambda");

    return "OK";
}
