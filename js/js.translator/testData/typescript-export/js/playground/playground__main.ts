import IFoo = JS_TESTS.foo.IFoo;
import ExportedParent = JS_TESTS.foo.ExportedParent;
import justCallFoo = JS_TESTS.foo.justCallFoo;
import callingWithDefaultsWithParameter = JS_TESTS.foo.callingWithDefaultsWithParameter;
import callingWithDefaultsWithoutParameter = JS_TESTS.foo.callingWithDefaultsWithoutParameter;
import callingWithBridge = JS_TESTS.foo.callingWithBridge;
import checkIsInterface = JS_TESTS.foo.checkIsInterface;
import callingWithDefaultImplementations = JS_TESTS.foo.callingWithDefaultImplementations;
import KtList = JS_TESTS.kotlin.collections.KtList;
import callingExportedParentMethod = JS_TESTS.foo.callingExportedParentMethod;
import justCallParentAsyncMethod = JS_TESTS.foo.justCallParentAsyncMethod;
import justCallAsyncFoo = JS_TESTS.foo.justCallAsyncFoo;
import KotlinFooImpl = JS_TESTS.foo.KotlinFooImpl;

class TsFooImpl implements IFoo<string> {
    foo(): string { return "OK" }

    async asyncFoo(): Promise<string> { return "OK" }

    withDefaults(value: string = "HEH"): string {
        return `TYPESCRIPT SIDE ${value}`
    }

    withBridge(x: string): string {
        return `TYPESCRIPT: ${x}`
    }

    anotherParentMethod(): KtList<string> {
        return KtList.fromJsArray(["OK"])
    }

    async parentAsyncMethod(): Promise<string> {
        return "Parent OK"
    }

    // TODO: there is a default implementations on the Kotlin side, but on TypeScript it's required
    withDefaultImplementation(): string {
        return ExportedParent.defaults.withDefaultImplementation(this)
    }

    suspendWithDefaultImplementation(): Promise<string> {
        return IFoo.defaults.suspendWithDefaultImplementation(this)
    }

    get propertyWithDefaultGetter(): string {
        return IFoo.defaults.propertyWithDefaultGetter.get(this)
    }

    get propertyWithDefaultSetter(): string {
        return ExportedParent.defaults.propertyWithDefaultSetter.get(this)
    }

    set propertyWithDefaultSetter(value: string) {
        ExportedParent.defaults.propertyWithDefaultSetter.set(this, value)
    }

    setGetterAndSetterWithJsName(value: string): void {
        ExportedParent.defaults.setGetterAndSetterWithJsName(this, value)
    }

    getGetterAndSetterWithJsName(): string {
        return ExportedParent.defaults.getGetterAndSetterWithJsName(this)
    }
}


/**
 * Not-solved but has a good-enough solution:
 * - Check is interface (P2)
 *
 * Solved problems
 * - Problem: It's possible to export interface with a not-exported parent (P1)
 *   Solution: It would be a new frontend check the same as we have for interface visibility.
 *
 * - Problem: Default value declared on the Kotlin side is always overridden by TypeScript implementation (P3)
 *   Solution: The problem is the same as for class exports, so we're going to ignore it for now.
 *
 * - Problem: Suspend functions don't work (P0)
 *   Solution: We're generating extra bridge for calling interface virtual methods. It works for both Kotlin and TypeScript implementations
 *
 * - Problem: No default implementations
 *   Solution: We're going to introduce a namespace called "defaults" which contains all the default implementations.
 */
async function testFoo(foo: IFoo<string>, languageImplemented: string): Promise<string> {
    let result;

    result = foo.foo();
    if (result !== "OK") return "Fail: just calling foo method returns unexpected result: " + result

    result = justCallFoo(foo)
    if (result !== "OK") return "Fail: providing FooImpl to justCallFoo returns unexpected result: " + result

    result = await foo.asyncFoo()
    if (result !== "OK") return "Fail: just calling asyncFoo method returns unexpected result: " + result

    result = await justCallAsyncFoo(foo)
    if (result !== "OK") return "Fail: providing FooImpl to justCallAsyncFoo returns unexpected result: " + result

    result = foo.withDefaults("CALL SIDE OK")
    if (result !== `${languageImplemented} SIDE CALL SIDE OK`) return "Fail: just calling withDefaults method with parameters returns unexpected result: " + result

    result = foo.withDefaults()
    const defaultValue = languageImplemented === "TYPESCRIPT" ? "HEH" : "OK"
    if (result !== `${languageImplemented} SIDE ${defaultValue}`) return "Fail: just calling withDefaults method without parameters returns unexpected result: " + result

    result = callingWithDefaultsWithParameter(foo)
    if (result !== `${languageImplemented} SIDE KOTLIN SIDE PARAMETER`) return "Fail: just calling callingWithDefaultsWithParameter returns unexpected result: " + result

    result = callingWithDefaultsWithoutParameter(foo)
    if (result !== `${languageImplemented} SIDE ${defaultValue}`) return "Fail: just calling callingWithDefaultsWithoutParameter returns unexpected result: " + result

    result = foo.withBridge("BRIDGE")
    if (result !== `${languageImplemented}: BRIDGE`) return "Fail: just calling withBridge method returns unexpected result: " + result

    result = callingWithBridge(foo)
    if (result !== `${languageImplemented}: KOTLIN SIDE`) return "Fail: just calling callingWithBridge returns unexpected result: " + result

    // TODO: uncomment when is checks will work
    // if (!checkIsInterface(foo)) return "Fail: foo failed `is`-check on IFoo on the Kotlin side"

    result = foo.withDefaultImplementation()
    if (result !== "KOTLIN IMPLEMENTATION: OK") return "Fail: just calling withDefaultImplementation method returns unexpected result: " + result

    result = callingWithDefaultImplementations(foo)
    if (result !== "KOTLIN IMPLEMENTATION: OK") return "Fail: just calling callingWithDefaultImplementations returns unexpected result: " + result

    result = foo.anotherParentMethod()
    if (result.asJsReadonlyArrayView()[0] != "OK") return "Fail: just calling anotherParentMethod returns unexpected result: " + result

    result = callingExportedParentMethod(foo)
    if (result !== "OK") return "Fail: just calling callingExportedParentMethod returns unexpected result: " + result

    result = await foo.parentAsyncMethod()
    if (result !== "Parent OK") return "Fail: just calling parentAsyncMethod returns unexpected result: " + result

    result = await justCallParentAsyncMethod(foo)
    if (result !== "Parent OK") return "Fail: just calling justCallParentAsyncMethod returns unexpected result: " + result

    result = foo.propertyWithDefaultSetter
    if (result !== "KOTLIN IMPLEMENTATION OK") return "Fail: just calling propertyWithDefaultSetter returns unexpected result: " + result

    foo.propertyWithDefaultSetter = "42"

    result = foo.propertyWithDefaultSetter
    if (result !== "KOTLIN IMPLEMENTATION OK") return "Fail: just calling propertyWithDefaultSetter returns unexpected result: " + result

    result = foo.propertyWithDefaultGetter
    if (result !== "KOTLIN IMPLEMENTATION KOTLIN IMPLEMENTATION OK") return "Fail: just calling propertyWithDefaultGetter returns unexpected result: " + result


    result = foo.getGetterAndSetterWithJsName()
    if (result !== "KOTLIN IMPLEMENTATION OK") return "Fail: just calling getGetterAndSetterWithJsName returns unexpected result: " + result

    foo.setGetterAndSetterWithJsName("test")

    result = foo.getGetterAndSetterWithJsName()
    if (result !== "KOTLIN IMPLEMENTATION OK") return "Fail: just calling getGetterAndSetterWithJsName returns unexpected result: " + result

    return "OK"
}

async function box(): Promise<string> {
    let result = await testFoo(new TsFooImpl(), "TYPESCRIPT")
    if (result !== "OK") return result

    result = await testFoo(new KotlinFooImpl(), "KOTLIN");
    if (result !== "OK") return result

    return "OK"
}