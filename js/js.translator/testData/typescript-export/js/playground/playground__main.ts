import IFoo = JS_TESTS.foo.IFoo;
import justCallFoo = JS_TESTS.foo.justCallFoo;
import callingWithDefaultsWithParameter = JS_TESTS.foo.callingWithDefaultsWithParameter;
import callingWithDefaultsWithoutParameter = JS_TESTS.foo.callingWithDefaultsWithoutParameter;
import callingWithBridge = JS_TESTS.foo.callingWithBridge;
import checkIsInterface = JS_TESTS.foo.checkIsInterface;
import callingWithDefaultImplementations = JS_TESTS.foo.callingWithDefaultImplementations;
import callingHiddenParentMethod = JS_TESTS.foo.callingHiddenParentMethod;
import KtList = JS_TESTS.kotlin.collections.KtList;
import callingExportedParentMethod = JS_TESTS.foo.callingExportedParentMethod;
import justCallParentAsyncMethod = JS_TESTS.foo.justCallParentAsyncMethod;

class FooImpl implements IFoo<string> {
    foo(): string { return "OK" }

    async asyncFoo(): Promise<string> { return "OK" }

    withDefaults(value?: string): string {
        return value ?? "TYPESCRIPT SIDE OK"
    }

    withBridge(x: string): string {
        return `TYPESCRIPT: ${x}`
    }

    // TODO: there is a default implementations on the Kotlin side, but on TypeScript it's required
    withDefaultImplementation(): string {
        return "OVERRIDDEN TYPESCRIPT IMPLEMENTATION"
    }

    anotherParentMethod(): KtList<string> {
        return KtList.fromJsArray(["OK"])
    }

    async parentAsyncMethod(): Promise<string> {
        return "Parent OK"
    }
}


/**
 * # Existing problems:
 * - Suspend functions don't work (P0)
 * - Check is interface (P2)
 *
 * Solved/Ignored problems
 * - Problem: Default value declared on the Kotlin side is always overridden by TypeScript implementation (P3)
 *   Solution: The problem is the same as for class exports, so we're going to ignore it for now.
 *
 * - Problem: It's possible to export interface with a not-exported parent (P1)
 *   Solution: It would be a new frontend check the same as we have for interface visibility.
 *
 * - Problem: No default implementations
 *   Solution: We're going to introduce a namespace called "defaults" which containing all the default implementations.
 */
async function box(): Promise<string> {
    const foo = new FooImpl();
    let result;

    result = foo.foo();
    if (result !== "OK") return "Fail: just calling foo method returns unexpected result: " + result

    result = justCallFoo(foo)
    if (result !== "OK") return "Fail: providing FooImpl to justCallFoo returns unexpected result: " + result

    result = await foo.asyncFoo()
    if (result !== "OK") return "Fail: just calling asyncFoo method returns unexpected result: " + result

    // TODO: uncomment when suspend functions implementation will work
    // result = await justCallAsyncFoo(foo)
    // if (result !== "OK") return "Fail: providing FooImpl to justCallAsyncFoo returns unexpected result: " + result

    result = foo.withDefaults("CALL SIDE OK")
    if (result !== "CALL SIDE OK") return "Fail: just calling withDefaults method with parameters returns unexpected result: " + result

    result = foo.withDefaults()
    if (result !== "TYPESCRIPT SIDE OK") return "Fail: just calling withDefaults method without parameters returns unexpected result: " + result

    result = callingWithDefaultsWithParameter(foo)
    if (result !== "KOTLIN SIDE PARAMETER") return "Fail: just calling callingWithDefaultsWithParameter returns unexpected result: " + result

    result = callingWithDefaultsWithoutParameter(foo)
    if (result !== "TYPESCRIPT SIDE OK") return "Fail: just calling callingWithDefaultsWithParameter returns unexpected result: " + result
    
    result = foo.withBridge("BRIDGE")
    if (result !== "TYPESCRIPT: BRIDGE") return "Fail: just calling withBridge method returns unexpected result: " + result
    
    result = callingWithBridge(foo)
    if (result !== "TYPESCRIPT: KOTLIN SIDE") return "Fail: just calling callingWithBridge returns unexpected result: " + result

    // TODO: uncomment when is checks will work
    // if (!checkIsInterface(foo)) return "Fail: foo failed `is`-check on IFoo on the Kotlin side"

    result = foo.withDefaultImplementation()
    if (result !== "OVERRIDDEN TYPESCRIPT IMPLEMENTATION") return "Fail: just calling withDefaultImplementation method returns unexpected result: " + result

    result = callingWithDefaultImplementations(foo)
    if (result !== "OVERRIDDEN TYPESCRIPT IMPLEMENTATION") return "Fail: just calling callingWithDefaultImplementations returns unexpected result: " + result

    // TODO: uncomment when it would be forbidden to export interface with a hidden parent
    // result = callingHiddenParentMethod(foo)
    // if (result !== 42) return "Fail: just calling callingHiddenParentMethod returns unexpected result: " + result

    result = foo.anotherParentMethod()
    if (result.asJsReadonlyArrayView()[0] != "OK") return "Fail: just calling anotherParentMethod returns unexpected result: " + result

    result = callingExportedParentMethod(foo)
    if (result !== "OK") return "Fail: just calling callingExportedParentMethod returns unexpected result: " + result

    result = await foo.parentAsyncMethod()
    if (result !== "Parent OK") return "Fail: just calling parentAsyncMethod returns unexpected result: " + result

    // TODO: uncomment when suspend functions implementation will work
    // result = await justCallParentAsyncMethod(foo)
    // if (result !== "Parent OK") return "Fail: just calling justCallParentAsyncMethod returns unexpected result: " + result

    return "OK"
}