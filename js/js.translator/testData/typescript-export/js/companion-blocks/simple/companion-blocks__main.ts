import ExportedWithCompanionBlock = JS_TESTS.foo.ExportedWithCompanionBlock;
import ExportedBase = JS_TESTS.foo.ExportedBase;
import ExportedChild = JS_TESTS.foo.ExportedChild;
import ExportedInterfaceWithCompanion = JS_TESTS.foo.ExportedInterfaceWithCompanion;

function assert(condition: boolean, message: string) {
    if (!condition) {
        throw `FAIL: ${message}`;
    }
}

function box(): string {
    assert(ExportedWithCompanionBlock.readOnly === "O", "readOnly");
    assert(ExportedWithCompanionBlock.mutable === "", "mutable initial");
    assert(ExportedWithCompanionBlock.append() === "OK", "append default");
    assert(ExportedWithCompanionBlock.mutable === "K", "mutable after append");
    ExportedWithCompanionBlock.mutable = "Q";
    assert(ExportedWithCompanionBlock.mutable === "Q", "mutable write");
    assert(ExportedWithCompanionBlock.append("L") === "OL", "append argument");
    assert(ExportedWithCompanionBlock.mutable === "L", "mutable after argument");

    const instance = new ExportedWithCompanionBlock();
    assert(instance.instanceReadOnly === "I", "instanceReadOnly");
    assert(instance.instanceMutable === "", "instance mutable initial");
    assert(instance.appendToInstance() === "IK", "instance append default");
    assert(instance.instanceMutable === "K", "instance mutable after append");
    instance.instanceMutable = "Q";
    assert(instance.instanceMutable === "Q", "instance mutable write");
    assert(instance.appendToInstance("L") === "IL", "instance append argument");
    assert(instance.instanceMutable === "L", "instance mutable after argument");

    assert(ExportedBase.shared() === "base", "base shared");
    assert(ExportedBase.baseOnly() === "baseOnly", "base only");
    assert(ExportedChild.shared() === "child", "child shared");
    assert(ExportedChild.childOnly() === "childOnly", "child only");
    assert(ExportedInterfaceWithCompanion.interfaceFun() === "interface", "interface fun");

    return "OK";
}
