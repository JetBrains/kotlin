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
import callGenericWithDefaultImplementation = JS_TESTS.foo.callGenericWithDefaultImplementation;
import callingDelegatingToSuperDefaultImplementation = JS_TESTS.foo.callingDelegatingToSuperDefaultImplementation;
import FunIFace = JS_TESTS.foo.FunIFace;
import makeFunInterfaceWithSam = JS_TESTS.foo.makeFunInterfaceWithSam;
import callFunInterface = JS_TESTS.foo.callFunInterface;
import NoRuntimeIface = JS_TESTS.foo.NoRuntimeIface;
import ChildOfNoRuntime = JS_TESTS.foo.ChildOfNoRuntime;
import KotlinNoRuntimeImpl = JS_TESTS.foo.KotlinNoRuntimeImpl;
import KotlinChildNoRuntimeImpl = JS_TESTS.foo.KotlinChildNoRuntimeImpl;
import NoRuntimeFunIface = JS_TESTS.foo.NoRuntimeFunIface;
import callNoRuntimeFunInterface = JS_TESTS.foo.callNoRuntimeFunInterface;
import makeNoRuntimeFunInterfaceWithSam = JS_TESTS.foo.makeNoRuntimeFunInterfaceWithSam;

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

    genericWithDefaultImplementation<T>(x: T): string {
        return IFoo.DefaultImpls.genericWithDefaultImplementation(this, x)
    }

    delegatingToSuperDefaultImplementation(): string {
        return IFoo.DefaultImpls.delegatingToSuperDefaultImplementation(this)
    }
}

class TsFunImpl implements FunIFace {
    readonly [FunIFace.Symbol] = true

    apply(x: string): string {
        return `TS ${x}`
    }
}

class TsNoRuntimeFunImpl implements NoRuntimeFunIface {
    run(): Array<string> {
        return ["SAM from TypeScript"];
    }
}

// TypeScript-side implementations for @JsNoRuntime interfaces
class TsNoRuntimeImpl implements NoRuntimeIface {
    constructor(public readonly a: string) {}
}

class TsChildNoRuntimeImpl implements ChildOfNoRuntime {
    constructor(public readonly a: string) {}
    child(): string { return `child-${this.a}` }
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

    result = foo.delegatingToSuperDefaultImplementation()
    if (result !== "KOTLIN IMPLEMENTATION: OK") return "Fail: just calling delegatingToSuperDefaultImplementation returns unexpected result: " + result

    result = callingDelegatingToSuperDefaultImplementation(foo)
    if (result !== "KOTLIN IMPLEMENTATION: OK") return "Fail: just calling callingDelegatingToSuperDefaultImplementation returns unexpected result: " + result

    result = foo.anotherDefaultImplementation()
    if (result !== "FROM IFoo") return "Fail: just calling callingAnotherWithDefaultImplementation method returns unexpected result: " + result

    result = callingAnotherWithDefaultImplementation(foo)
    if (result !== "FROM IFoo") return "Fail: just calling callingAnotherWithDefaultImplementation returns unexpected result: " + result

    result = foo.genericWithDefaultImplementation("OK")
    if (result !== "GENERIC OK") return "Fail: just calling genericWithDefaultImplementation returns unexpected result: " + result

    result = callGenericWithDefaultImplementation(foo, "OK")
    if (result !== "GENERIC OK") return "Fail: just calling callGenericWithDefaultImplementation returns unexpected result: " + result

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

function testFunInterface(f: FunIFace, expectedPrefix: string): string {
    let result: any

    result = f.apply("OK")
    if (result !== `${expectedPrefix} OK`) return "Fail: calling f.apply returns unexpected result: " + result

    result = callFunInterface(f, "OK")
    if (result !== `${expectedPrefix} OK`) return "Fail: calling callFunInterface with f returns unexpected result: " + result

    return "OK"
}

function testNoRuntimeFunInterface(f: NoRuntimeFunIface, expectedSuffix: string): string {
    let result: any

    result = f.run()[0]
    if (result !== `SAM from ${expectedSuffix}`) return "Fail: calling f.run returns unexpected result: " + result

    result = callNoRuntimeFunInterface(f)[0]
    if (result !== `SAM from ${expectedSuffix}`) return "Fail: calling callFunInterface with f returns unexpected result: " + result

    return "OK"
}

async function box(): Promise<string> {
    let result = await testFoo(new TsFooImpl(), "TYPESCRIPT")
    if (result !== "OK") return result

    result = await testFoo(new KotlinFooImpl(), "KOTLIN");
    if (result !== "OK") return result

    let funResult = testFunInterface(makeFunInterfaceWithSam(), "SAM")
    if (funResult !== "OK") return funResult

    funResult = testFunInterface(new TsFunImpl(), "TS")
    if (funResult !== "OK") return funResult

    let noRuntimeFunResult = testNoRuntimeFunInterface(makeNoRuntimeFunInterfaceWithSam(), "Kotlin")
    if (noRuntimeFunResult !== "OK") return noRuntimeFunResult

    noRuntimeFunResult = testNoRuntimeFunInterface(new TsNoRuntimeFunImpl(), "TypeScript")
    if (noRuntimeFunResult !== "OK") return noRuntimeFunResult

    const tsNR: NoRuntimeIface = new TsNoRuntimeImpl("X")
    if (tsNR.a !== "X") return "Fail: TsNoRuntimeImpl.a is wrong: " + tsNR.a

    const tsChildNR: ChildOfNoRuntime = new TsChildNoRuntimeImpl("Y")
    if (tsChildNR.child() !== "child-Y") return "Fail: TsChildNoRuntimeImpl.child() is wrong: " + tsChildNR.child()

    const ktNR: NoRuntimeIface = new KotlinNoRuntimeImpl("K")
    if (ktNR.a !== "K") return "Fail: KotlinNoRuntimeImpl.a is wrong: "+ ktNR.a

    const ktChildNR: ChildOfNoRuntime = new KotlinChildNoRuntimeImpl("Z")
    if (ktChildNR.child() !== "child-Z") return "Fail: KotlinChildNoRuntimeImpl.child() is wrong: " + ktChildNR.child()

    return "OK"
}
