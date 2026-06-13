import exportedSuspendLambda = JS_TESTS.foo.exportedSuspendLambda;
import produceSuspendLambda = JS_TESTS.foo.produceSuspendLambda;
import produceCapturingSuspendLambda = JS_TESTS.foo.produceCapturingSuspendLambda;
import runLambda = JS_TESTS.foo.runLambda;
import runVoidLambda = JS_TESTS.foo.runVoidLambda;
import chain = JS_TESTS.foo.chain;
import roundTrip = JS_TESTS.foo.roundTrip;
import LambdaHolder = JS_TESTS.foo.LambdaHolder;
import callKotlinLambdaFromKotlin = JS_TESTS.foo.callKotlinLambdaFromKotlin;
import produceArrayOfSuspendLambdas = JS_TESTS.foo.produceArrayOfSuspendLambdas;
import reduceArrayOfSuspendLambdas = JS_TESTS.foo.reduceArrayOfSuspendLambdas;
import mapWithArrayOfSuspendLambdas = JS_TESTS.foo.mapWithArrayOfSuspendLambdas;
import getSuspendDoubleRef = JS_TESTS.foo.getSuspendDoubleRef;
import getSuspendIncRef = JS_TESTS.foo.getSuspendIncRef;
import topLevelSuspendInc = JS_TESTS.foo.topLevelSuspendInc;
import WithSuspendMethod = JS_TESTS.foo.WithSuspendMethod;
import InterfaceWithSuspendLambdaProp = JS_TESTS.foo.InterfaceWithSuspendLambdaProp;
import AbstractClassWithSuspendLambdaProp = JS_TESTS.foo.AbstractClassWithSuspendLambdaProp;
import callHandlerFromInterface = JS_TESTS.foo.callHandlerFromInterface;
import callHandlerFromAbstractClass = JS_TESTS.foo.callHandlerFromAbstractClass;

class TSInterfaceImpl implements InterfaceWithSuspendLambdaProp {
    readonly handler = async (x: number) => "value:" + x;
}

class TSAbstractClassImpl extends AbstractClassWithSuspendLambdaProp {
    get handler() { return async (x: number) => "abstract:" + x; }
}

function assert(condition: boolean, label: string = "") {
    if (!condition) {
        throw "Assertion failed: " + label;
    }
}

async function box(): Promise<string> {
    assert(await exportedSuspendLambda() === "OK", "exportedSuspendLambda");

    const doubler = produceSuspendLambda();
    assert(await doubler(21) === 42, "produceSuspendLambda");

    const adder = produceCapturingSuspendLambda(10);
    assert(await adder(5) === 15, "produceCapturingSuspendLambda");

    assert(await runLambda(async (x: number) => x + 21) === 84, "runLambda");

    let voidLambdaCalled = false;
    await runVoidLambda(async () => { voidLambdaCalled = true; });
    assert(voidLambdaCalled, "runVoidLambda");

    assert(await chain(async (x: number) => x + 1, async (x: number) => x * 3, 4) === 15, "chain");

    const back = roundTrip(async (x: number) => x * 2);
    assert(await back(10) === 21, "roundTrip");

    const holder = new LambdaHolder(100);
    assert(await holder.multiplier(3, 4) === 12, "holder.multiplier");
    assert(await holder.apply(async (x: number) => x + 1, 41) === 42, "holder.apply");
    assert(await holder.applyTwice(async (x: number) => x + 1, 40) === 42, "holder.applyTwice");

    const adderFromHolder = holder.produceAdder();
    assert(await adderFromHolder(2) === 102, "holder.produceAdder");

    assert(await callKotlinLambdaFromKotlin() === 10, "callKotlinLambdaFromKotlin");

    // Arrays of suspend lambdas
    const kotlinLambdas = produceArrayOfSuspendLambdas();
    assert(kotlinLambdas.length === 3, "produceArrayOfSuspendLambdas length");
    assert(await kotlinLambdas[0](10) === 11, "produceArrayOfSuspendLambdas[0]");
    assert(await kotlinLambdas[1](10) === 20, "produceArrayOfSuspendLambdas[1]");
    assert(await kotlinLambdas[2](10) === 100, "produceArrayOfSuspendLambdas[2]");

    assert(await reduceArrayOfSuspendLambdas(kotlinLambdas, 2) === 36, "reduceArrayOfSuspendLambdas (kotlin)");

    const tsLambdas = [
        async (x: number) => x + 5,
        async (x: number) => x * 3,
    ];
    assert(await reduceArrayOfSuspendLambdas(tsLambdas, 1) === 18, "reduceArrayOfSuspendLambdas (ts)");

    const mapped = await mapWithArrayOfSuspendLambdas(kotlinLambdas, 4);
    assert(mapped.length === 3, "mapWithArrayOfSuspendLambdas length");
    assert(mapped[0] === 5 && mapped[1] === 8 && mapped[2] === 16, "mapWithArrayOfSuspendLambdas values");

    const mappedTs = await mapWithArrayOfSuspendLambdas(tsLambdas, 10);
    assert(mappedTs[0] === 15 && mappedTs[1] === 30, "mapWithArrayOfSuspendLambdas (ts values)");

    // Callable references to suspend functions returned as suspend lambdas
    const doubleRef = getSuspendDoubleRef();
    assert(await doubleRef(21) === 42, "getSuspendDoubleRef");

    const incRef = getSuspendIncRef();
    assert(await incRef(41) === 42, "getSuspendIncRef");

    assert(await topLevelSuspendInc(99) === 100, "topLevelSuspendInc direct");

    const withMethod = new WithSuspendMethod(7);
    const memberRef = withMethod.memberRef();
    assert(await memberRef(35) === 42, "WithSuspendMethod.memberRef");

    // TypeScript override of suspend lambda property from Kotlin interface
    const tsInterfaceImpl = new TSInterfaceImpl();
    assert(await callHandlerFromInterface(tsInterfaceImpl, 5) === "value:5", "callHandlerFromInterface");

    // TypeScript override of suspend lambda property from Kotlin abstract class
    const tsAbstractImpl = new TSAbstractClassImpl();
    assert(await callHandlerFromAbstractClass(tsAbstractImpl, 7) === "abstract:7", "callHandlerFromAbstractClass");

    return "OK";
}
