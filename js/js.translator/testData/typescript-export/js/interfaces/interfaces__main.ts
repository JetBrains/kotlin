import TestInterfaceImpl = JS_TESTS.foo.TestInterfaceImpl;
import ChildTestInterfaceImpl = JS_TESTS.foo.ChildTestInterfaceImpl;
import processInterface = JS_TESTS.foo.processInterface;
import processOptionalInterface = JS_TESTS.foo.processOptionalInterface;
import WithTheCompanion = JS_TESTS.foo.WithTheCompanion;
import InterfaceWithJsStaticVar = JS_TESTS.foo.InterfaceWithJsStaticVar;
import InterfaceWithNamedCompanion = JS_TESTS.foo.InterfaceWithNamedCompanion;
import ImplementorOfInterfaceWithDefaultArguments = JS_TESTS.foo.ImplementorOfInterfaceWithDefaultArguments;
import SomeSealedInterface = JS_TESTS.foo.SomeSealedInterface
import NoRuntimeSimpleInterface = JS_TESTS.foo.NoRuntimeSimpleInterface
import NRBase = JS_TESTS.foo.NRBase
import WithDefaultSuspend = JS_TESTS.foo.WithDefaultSuspend;
import WithDefaultSuspendImpl = JS_TESTS.foo.WithDefaultSuspendImpl;
import AbstractAndDefaultSuspend = JS_TESTS.foo.AbstractAndDefaultSuspend;
import AbstractAndDefaultSuspendImpl = JS_TESTS.foo.AbstractAndDefaultSuspendImpl;
import callComposedDefaultSuspend = JS_TESTS.foo.callComposedDefaultSuspend;
import ChainedDefaultSuspend = JS_TESTS.foo.ChainedDefaultSuspend;
import ChainedDefaultSuspendImpl = JS_TESTS.foo.ChainedDefaultSuspendImpl;
import callOuterDefaultSuspend = JS_TESTS.foo.callOuterDefaultSuspend;
import BaseDiamondDefaultSuspend = JS_TESTS.foo.BaseDiamondDefaultSuspend;
import LeftDiamondDefaultSuspend = JS_TESTS.foo.LeftDiamondDefaultSuspend;
import RightDiamondDefaultSuspend = JS_TESTS.foo.RightDiamondDefaultSuspend;
import DiamondDefaultSuspendImpl = JS_TESTS.foo.DiamondDefaultSuspendImpl;
import callDiamondDefaultSuspend = JS_TESTS.foo.callDiamondDefaultSuspend;
import GenericDefaultSuspend = JS_TESTS.foo.GenericDefaultSuspend;
import StringGenericDefaultSuspendImpl = JS_TESTS.foo.StringGenericDefaultSuspendImpl;
import callGenericDefaultSuspend = JS_TESTS.foo.callGenericDefaultSuspend;
import ChainDefaultSuspend = JS_TESTS.foo.ChainDefaultSuspend;
import MidChainDefaultSuspendImpl = JS_TESTS.foo.MidChainDefaultSuspendImpl;
import LeafChainDefaultSuspendImpl = JS_TESTS.foo.LeafChainDefaultSuspendImpl;
import callChainDefaultSuspend = JS_TESTS.foo.callChainDefaultSuspend;
import LeftDefaultSuspend = JS_TESTS.foo.LeftDefaultSuspend;
import RightDefaultSuspend = JS_TESTS.foo.RightDefaultSuspend;
import MultipleInterfaceDefaultsImpl = JS_TESTS.foo.MultipleInterfaceDefaultsImpl;
import callLeftDefaultSuspend = JS_TESTS.foo.callLeftDefaultSuspend;
import callRightDefaultSuspend = JS_TESTS.foo.callRightDefaultSuspend;
import NullableDefaultSuspend = JS_TESTS.foo.NullableDefaultSuspend;
import NullableDefaultSuspendImpl = JS_TESTS.foo.NullableDefaultSuspendImpl;
import callNullableDefaultSuspend = JS_TESTS.foo.callNullableDefaultSuspend;
import ParameterizedDefaultSuspend = JS_TESTS.foo.ParameterizedDefaultSuspend;
import ParameterizedDefaultSuspendImpl = JS_TESTS.foo.ParameterizedDefaultSuspendImpl;
import callParameterizedDefaultSuspend = JS_TESTS.foo.callParameterizedDefaultSuspend;
import UnitDefaultSuspend = JS_TESTS.foo.UnitDefaultSuspend;
import UnitDefaultSuspendImpl = JS_TESTS.foo.UnitDefaultSuspendImpl;
import callUnitDefaultSuspend = JS_TESTS.foo.callUnitDefaultSuspend;
import ExportedSuspendChild = JS_TESTS.foo.ExportedSuspendChild;
import HolderOfInheritedSuspend = JS_TESTS.foo.HolderOfInheritedSuspend;
import callParentSuspend = JS_TESTS.foo.callParentSuspend;
import OverridableSuspend = JS_TESTS.foo.OverridableSuspend;
import InheritingSuspendImpl = JS_TESTS.foo.InheritingSuspendImpl;
import OverridingSuspendImpl = JS_TESTS.foo.OverridingSuspendImpl;
import callOverrideSuspend = JS_TESTS.foo.callOverrideSuspend;

function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

async function box(): Promise<string> {
    assert(processInterface(new TestInterfaceImpl("bar")) === "Owner TestInterfaceImpl has value 'bar'")
    assert(processInterface(new ChildTestInterfaceImpl()) === "Owner TestInterfaceImpl has value 'Test'")

    // @ts-expect-error "Just test that this code will throw compilation error for a user"
    assert(processInterface({ value: "bar", getOwnerName: () => "RandomObject" }) === "Owner RandomObject has value 'bar'")

    assert(processOptionalInterface({ required: 4 }) == "4unknown")
    assert(processOptionalInterface({ required: 4, notRequired: null }) == "4unknown")
    assert(processOptionalInterface({ required: 4, notRequired: 5 }) == "45")

    assert(WithTheCompanion.companionStaticFunction() == "STATIC FUNCTION")
    assert(WithTheCompanion.Companion.companionFunction() == "FUNCTION")
    assert(InterfaceWithJsStaticVar.mutable == "INITIAL")
    InterfaceWithJsStaticVar.mutable = "UPDATED"
    assert(InterfaceWithJsStaticVar.mutable == "UPDATED")
    assert(InterfaceWithNamedCompanion.companionStaticFunction() == "STATIC FUNCTION")
    assert(InterfaceWithNamedCompanion.Named.companionFunction() == "FUNCTION")

    const instance = new ImplementorOfInterfaceWithDefaultArguments()
    assert(instance.foo() === 0);
    assert(instance.foo(2) === 2);
    assert(instance.bar() === 1);
    assert(instance.bar(2) === 3);

    const sealedImpl: SomeSealedInterface = new SomeSealedInterface.SomeNestedImpl("OK")
    assert(sealedImpl.x === "OK")

    const nrSimple: NoRuntimeSimpleInterface = { x: "ok" }
    assert(nrSimple.x === "ok")

    const nrBase: NRBase = { b: "base" }
    assert(nrBase.b === "base")

    const withDefaultSuspendImpl = new WithDefaultSuspendImpl()
    assert(typeof WithDefaultSuspendImpl.prototype.regularWithDefault === "function")
    assert(typeof WithDefaultSuspendImpl.prototype.suspendWithDefault === "function")
    assert(typeof withDefaultSuspendImpl.regularWithDefault === "function")
    assert(typeof withDefaultSuspendImpl.suspendWithDefault === "function")
    assert(withDefaultSuspendImpl.regularWithDefault() === "OK")
    assert(await withDefaultSuspendImpl.suspendWithDefault() === "OK")

    const withDefaultSuspend: WithDefaultSuspend = withDefaultSuspendImpl
    assert(withDefaultSuspend.regularWithDefault() === "OK")
    assert(await withDefaultSuspend.suspendWithDefault() === "OK")

    const abstractAndDefaultImpl = new AbstractAndDefaultSuspendImpl()
    assert(await abstractAndDefaultImpl.abstractSuspend() === "ABSTRACT")
    assert(await abstractAndDefaultImpl.defaultSuspend() === "ABSTRACT DEFAULT")

    const abstractAndDefault: AbstractAndDefaultSuspend = abstractAndDefaultImpl
    assert(await abstractAndDefault.abstractSuspend() === "ABSTRACT")
    assert(await abstractAndDefault.defaultSuspend() === "ABSTRACT DEFAULT")
    assert(await callComposedDefaultSuspend(abstractAndDefault) === "ABSTRACT DEFAULT")

    const chainedDefaultImpl = new ChainedDefaultSuspendImpl()
    assert(await chainedDefaultImpl.innerSuspendDefault() === "INNER")
    assert(await chainedDefaultImpl.outerSuspendDefault() === "INNER OUTER")

    const chainedDefault: ChainedDefaultSuspend = chainedDefaultImpl
    assert(await chainedDefault.innerSuspendDefault() === "INNER")
    assert(await chainedDefault.outerSuspendDefault() === "INNER OUTER")
    assert(await callOuterDefaultSuspend(chainedDefault) === "INNER OUTER")

    const diamondDefaultImpl = new DiamondDefaultSuspendImpl()
    assert(await diamondDefaultImpl.suspendDefault() === "DIAMOND")

    const leftDiamond: LeftDiamondDefaultSuspend = diamondDefaultImpl
    assert(await leftDiamond.suspendDefault() === "DIAMOND")

    const rightDiamond: RightDiamondDefaultSuspend = diamondDefaultImpl
    assert(await rightDiamond.suspendDefault() === "DIAMOND")

    const baseDiamond: BaseDiamondDefaultSuspend = diamondDefaultImpl
    assert(await baseDiamond.suspendDefault() === "DIAMOND")
    assert(await callDiamondDefaultSuspend(baseDiamond) === "DIAMOND")

    const genericDefaultImpl = new StringGenericDefaultSuspendImpl()
    assert(await genericDefaultImpl.echoSuspendDefault("CLASS") === "CLASS")

    const genericDefault: GenericDefaultSuspend<string> = genericDefaultImpl
    assert(await genericDefault.echoSuspendDefault("INTERFACE") === "INTERFACE")
    assert(await callGenericDefaultSuspend(genericDefault, "KOTLIN") === "KOTLIN")

    const midChainImpl = new MidChainDefaultSuspendImpl()
    assert(await midChainImpl.suspendDefault() === "CHAIN OK")
    assert(await midChainImpl.suspendDefault("MID") === "CHAIN MID")

    const leafChainImpl = new LeafChainDefaultSuspendImpl()
    assert(await leafChainImpl.suspendDefault() === "CHAIN OK")
    assert(await leafChainImpl.suspendDefault("LEAF") === "CHAIN LEAF")

    const chainDefault: ChainDefaultSuspend = leafChainImpl
    assert(await chainDefault.suspendDefault("INTERFACE") === "CHAIN INTERFACE")
    assert(await callChainDefaultSuspend(chainDefault, "KOTLIN") === "CHAIN KOTLIN")

    const multipleInterfaceDefaultsImpl = new MultipleInterfaceDefaultsImpl()
    assert(await multipleInterfaceDefaultsImpl.leftSuspendDefault() === "LEFT")
    assert(await multipleInterfaceDefaultsImpl.rightSuspendDefault() === "RIGHT")

    const leftDefault: LeftDefaultSuspend = multipleInterfaceDefaultsImpl
    assert(await leftDefault.leftSuspendDefault() === "LEFT")

    const rightDefault: RightDefaultSuspend = multipleInterfaceDefaultsImpl
    assert(await rightDefault.rightSuspendDefault() === "RIGHT")

    assert(await callLeftDefaultSuspend(leftDefault) === "LEFT")
    assert(await callRightDefaultSuspend(rightDefault) === "RIGHT")

    const nullableDefaultImpl = new NullableDefaultSuspendImpl()
    assert(await nullableDefaultImpl.suspendDefault() === null)

    const nullableDefault: NullableDefaultSuspend = nullableDefaultImpl
    assert(await nullableDefault.suspendDefault() === null)
    assert(await callNullableDefaultSuspend(nullableDefault) === null)

    const parameterizedDefaultImpl = new ParameterizedDefaultSuspendImpl()
    assert(await parameterizedDefaultImpl.suspendDefault() === "VALUE OK")
    assert(await parameterizedDefaultImpl.suspendDefault("TS") === "VALUE TS")

    const parameterizedDefault: ParameterizedDefaultSuspend = parameterizedDefaultImpl
    assert(await parameterizedDefault.suspendDefault() === "VALUE OK")
    assert(await parameterizedDefault.suspendDefault("INTERFACE") === "VALUE INTERFACE")
    assert(await callParameterizedDefaultSuspend(parameterizedDefault, "KOTLIN") === "VALUE KOTLIN")

    const unitDefaultImpl = new UnitDefaultSuspendImpl()
    await unitDefaultImpl.runDefault()

    const unitDefault: UnitDefaultSuspend = unitDefaultImpl
    await unitDefault.runDefault()
    await callUnitDefaultSuspend(unitDefault)

    const exportedSuspendChildImpl = new ExportedSuspendChild()
    assert(await exportedSuspendChildImpl.parentSuspend("DIRECT") === "PARENT DIRECT")
    assert(await exportedSuspendChildImpl.childSuspend() === "CHILD")

    const exportedSuspendChild: HolderOfInheritedSuspend = exportedSuspendChildImpl
    assert(await exportedSuspendChild.parentSuspend("TS") === "PARENT TS")
    assert(await callParentSuspend(exportedSuspendChild, "KOTLIN") === "PARENT KOTLIN")

    const inheritingSuspendImpl = new InheritingSuspendImpl()
    assert(await inheritingSuspendImpl.suspendDefault() === "DEFAULT")

    const inheritingSuspend: OverridableSuspend = inheritingSuspendImpl
    assert(await inheritingSuspend.suspendDefault() === "DEFAULT")
    assert(await callOverrideSuspend(inheritingSuspend) === "DEFAULT")

    const overridingSuspendImpl = new OverridingSuspendImpl()
    assert(await overridingSuspendImpl.suspendDefault() === "OVERRIDDEN")

    const overridingSuspend: OverridableSuspend = overridingSuspendImpl
    assert(await overridingSuspend.suspendDefault() === "OVERRIDDEN")
    assert(await callOverrideSuspend(overridingSuspend) === "OVERRIDDEN")

    return "OK";
}
