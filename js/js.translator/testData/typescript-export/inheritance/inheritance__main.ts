import OC = JS_TESTS.foo.OC;
import AC = JS_TESTS.foo.AC;
import FC = JS_TESTS.foo.FC;
import O1 = JS_TESTS.foo.O1;
import O2 = JS_TESTS.foo.O2;

class Impl extends AC {
    z(z: number): void {
    }

    get acAbstractProp(): string { return "Impl"; }
    get y(): boolean { return true; }
}

function box(): string {
    const impl = new Impl();
    if (impl.acProp !== "acProp") return "Fail 1";
    if (impl.x !== "AC") return "Fail 2";
    if (impl.acAbstractProp !== "Impl") return "Fail 2.1";
    if (impl.y !== true) return "Fail 2.2";


    const oc = new OC(false, "OC");
    if (oc.y !== false) return "Fail 3";
    if (oc.acAbstractProp !== "OC") return "Fail 4";
    oc.z(10);


    const fc = new FC();
    if (fc.y !== true) return "Fail 5";
    if (fc.acAbstractProp !== "FC") return "Fail 6";
    fc.z(10);

    if (O1.y !== true || O2.y !== true) return "Fail 7";
    if (O1.acAbstractProp != "O1") return "Fail 8";
    if (O2.acAbstractProp != "O2") return "Fail 9";
    if (O2.foo() != 10) return "Fail 10";

    return "OK";
}