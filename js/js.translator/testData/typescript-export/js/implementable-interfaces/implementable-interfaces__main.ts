import IFoo = JS_TESTS.foo.IFoo;
import ExportedParent = JS_TESTS.foo.ExportedParent;
import justCallFoo = JS_TESTS.foo.justCallFoo;
import callingWithDefaultsWithParameter = JS_TESTS.foo.callingWithDefaultsWithParameter;
import callingWithDefaultsWithoutParameter = JS_TESTS.foo.callingWithDefaultsWithoutParameter;
import callingWithBridge = JS_TESTS.foo.callingWithBridge;
import checkIsFooInterface = JS_TESTS.foo.checkIsFooInterface;
import checkIsExportedParentInterface = JS_TESTS.foo.checkIsExportedParentInterface;
import KtList = JS_TESTS.kotlin.collections.KtList;
import callingExportedParentMethod = JS_TESTS.foo.callingExportedParentMethod;
import KotlinFooImpl = JS_TESTS.foo.KotlinFooImpl;
import justCallParentAsyncMethod = JS_TESTS.foo.justCallParentAsyncMethod;
import justCallAsyncFoo = JS_TESTS.foo.justCallAsyncFoo;
import callingWithDefaultImplementation = JS_TESTS.foo.callingWithDefaultImplementation;
import justCallSuspendWithDefaultImplementation = JS_TESTS.foo.justCallSuspendWithDefaultImplementation;
import callingAnotherWithDefaultImplementation = JS_TESTS.foo.callingAnotherWithDefaultImplementation;
import callingWithDefaultsAndDefaultImplementationWithParameter = JS_TESTS.foo.callingWithDefaultsAndDefaultImplementationWithParameter;
import callingWithDefaultsAndDefaultImplementationWithoutParameter = JS_TESTS.foo.callingWithDefaultsAndDefaultImplementationWithoutParameter;

class TsFooImpl implements IFoo<string> {
    readonly [IFoo.Symbol] = true
    readonly [ExportedParent.Symbol] = true

    readonly fooProperty: string = "IMPLEMENTED BY TYPESCRIPT FOO PROPERTY";
    parentPropertyToImplement: string = "IMPLEMENTED BY TYPESCRIPT PARENT PROPERTY";

    setGetterAndSetterWithJsName(_set___: string): void { }
    getGetterAndSetterWithJsName(): string {
        return `TYPESCRIPT IMPLEMENTATION ${this.anotherParentMethod().asJsReadonlyArrayView()[0]}`
    }

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

    withDefaultsAndDefaultImplementation(value?: string): string {
        return IFoo.DefaultImpls.withDefaultsAndDefaultImplementation(this, value)
    }

    suspendWithDefaultImplementation(): Promise<string> {
        return IFoo.DefaultImpls.suspendWithDefaultImplementation(this)
    }

    get propertyWithDefaultGetter(): string {
        return IFoo.DefaultImpls.propertyWithDefaultGetter.get(this)
    }

    get propertyWithDefaultSetter(): string {
        return ExportedParent.DefaultImpls.propertyWithDefaultSetter.get(this)
    }

    set propertyWithDefaultSetter(value: string) {
        ExportedParent.DefaultImpls.propertyWithDefaultSetter.set(this, value)
    }

    withDefaultImplementation(): string {
        return ExportedParent.DefaultImpls.withDefaultImplementation(this)
    }

    setDefaultGetterAndSetterWithJsName(value: string): void {
        ExportedParent.DefaultImpls.setDefaultGetterAndSetterWithJsName(this, value)
    }

    getDefaultGetterAndSetterWithJsName(): string {
        return ExportedParent.DefaultImpls.getDefaultGetterAndSetterWithJsName(this)
    }

    anotherDefaultImplementation(): string {
        return IFoo.DefaultImpls.anotherDefaultImplementation(this)
    }
}

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

    result = await foo.suspendWithDefaultImplementation()
    if (result !== "KOTLIN IMPLEMENTATION OK") return "Fail: just calling suspendWithDefaultImplementation method returns unexpected result: " + result

    result = await justCallSuspendWithDefaultImplementation(foo)
    if (result !== "KOTLIN IMPLEMENTATION OK") return "Fail: providing FooImpl to justCallSuspendWithDefaultImplementation returns unexpected result: " + result

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

    result = callingWithDefaultsAndDefaultImplementationWithParameter(foo)
    if (result !== `KOTLIN SIDE PARAMETER`) return "Fail: just calling callingWithDefaultsAndDefaultImplementationWithParameter returns unexpected result: " + result

    result = callingWithDefaultsAndDefaultImplementationWithoutParameter(foo)
    if (result !== `OK`) return "Fail: just calling callingWithDefaultsAndDefaultImplementationWithoutParameter returns unexpected result: " + result

    result = foo.withDefaultsAndDefaultImplementation("TYPESCRIPT SIDE PARAMETER")
    if (result !== `TYPESCRIPT SIDE PARAMETER`) return "Fail: just calling withDefaultsAndDefaultImplementation returns unexpected result: " + result

    result = foo.withDefaultsAndDefaultImplementation()
    if (result !== `OK`) return "Fail: just calling withDefaultsAndDefaultImplementation returns unexpected result: " + result

    result = callingWithDefaultsWithoutParameter(foo)
    if (result !== `${languageImplemented} SIDE ${defaultValue}`) return "Fail: just calling callingWithDefaultsWithoutParameter returns unexpected result: " + result

    result = foo.withBridge("BRIDGE")
    if (result !== `${languageImplemented}: BRIDGE`) return "Fail: just calling withBridge method returns unexpected result: " + result

    result = callingWithBridge(foo)
    if (result !== `${languageImplemented}: KOTLIN SIDE`) return "Fail: just calling callingWithBridge returns unexpected result: " + result

    if (!checkIsFooInterface(foo)) return "Fail: foo failed `is`-check on IFoo on the Kotlin side"
    if (!checkIsExportedParentInterface(foo)) return "Fail: foo failed `is`-check on ExortedParent on the Kotlin side"

    result = foo.withDefaultImplementation()
    if (result !== "KOTLIN IMPLEMENTATION: OK") return "Fail: just calling withDefaultImplementation method returns unexpected result: " + result
    
    result = callingWithDefaultImplementation(foo)
    if (result !== "KOTLIN IMPLEMENTATION: OK") return "Fail: just calling callingWithDefaultImplementation returns unexpected result: " + result

    result = foo.anotherDefaultImplementation()
    if (result !== "FROM IFoo") return "Fail: just calling callingAnotherWithDefaultImplementation method returns unexpected result: " + result

    result = callingAnotherWithDefaultImplementation(foo)
    if (result !== "FROM IFoo") return "Fail: just calling callingAnotherWithDefaultImplementation returns unexpected result: " + result

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

    result = foo.parentPropertyToImplement
    if (result !== `IMPLEMENTED BY ${languageImplemented} PARENT PROPERTY`) return "Fail: just calling parentPropertyToImplement returns unexpected result: " + result

    foo.parentPropertyToImplement = "42"

    result = foo.parentPropertyToImplement
    if (result !== "42") return "Fail: just calling parentPropertyToImplement returns unexpected result: " + result

    result = foo.fooProperty
    if (result !== `IMPLEMENTED BY ${languageImplemented} FOO PROPERTY`) return "Fail: just calling propertyWithDefaultGetter returns unexpected result: " + result

    result = foo.getGetterAndSetterWithJsName()
    if (result !== `${languageImplemented} IMPLEMENTATION OK`) return "Fail: just calling getGetterAndSetterWithJsName returns unexpected result: " + result

    foo.setGetterAndSetterWithJsName("test")

    result = foo.getGetterAndSetterWithJsName()
    if (result !== `${languageImplemented} IMPLEMENTATION OK`) return "Fail: just calling getGetterAndSetterWithJsName returns unexpected result: " + result

    result = foo.getDefaultGetterAndSetterWithJsName()
    if (result !== "KOTLIN IMPLEMENTATION OK") return "Fail: just calling getGetterAndSetterWithJsName returns unexpected result: " + result

    foo.setDefaultGetterAndSetterWithJsName("test")

    result = foo.getDefaultGetterAndSetterWithJsName()
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
