import OC = JS_TESTS.foo.OC;
import AC = JS_TESTS.foo.AC;
import FC = JS_TESTS.foo.FC;
import O1 = JS_TESTS.foo.O1;
import O2 = JS_TESTS.foo.O2;
import getI3 = JS_TESTS.foo.getI3;
import getA = JS_TESTS.foo.getA;
import getB = JS_TESTS.foo.getB;
import getC = JS_TESTS.foo.getC;
import B2 = JS_TESTS.foo.B2;
import C2 = JS_TESTS.foo.C2;
import EC = JS_TESTS.foo.EC;
import A2 = JS_TESTS.foo.A2;
import O3 = JS_TESTS.foo.O3;

class Impl extends AC {
    z(z: number): void {
    }

    get acAbstractProp(): string { return "Impl"; }
    get y(): boolean { return true; }
}

class A2Impl extends A2 {
    _bar: string = "barA2"

    get bar(): string {
        return this._bar
    }
    set bar(value: string) {
        this._bar = value
    }
    get baz(): string {
        return "bazA2"
    }
    get foo(): string {
        return "fooA2"
    }

    bay(): string {
        return "bayA2";
    }

}

function box(): string {
    const impl = new Impl();
    if (impl.acProp !== "acProp") return "Error: AC implementation's acProp property should be 'acProp'";
    if (impl.x !== "AC") return "Error: AC implementation's x property should be 'AC'";
    if (impl.acAbstractProp !== "Impl") return "Error: AC implementation's acAbstractProp property should be 'Impl'";
    if (impl.y !== true) return "Error: AC implementation's y property should be true";


    const oc = new OC(false, "OC");
    if (oc.y !== false) return "Error: OC instance's y property should be false";
    if (oc.acAbstractProp !== "OC") return "Error: OC instance's acAbstractProp property should be 'OC'";
    oc.z(10);


    const fc = new FC();
    if (fc.y !== true) return "Error: FC instance's y property should be true";
    if (fc.acAbstractProp !== "FC") return "Error: FC instance's acAbstractProp property should be 'FC'";
    fc.z(10);

    if (O1.y !== true || O2.y !== true) return "Error: Object singletons O1 and O2 should have y property set to true";
    if (O1.acAbstractProp != "O1") return "Error: Object singleton O1 should have acAbstractProp property set to 'O1'";
    if (O2.acAbstractProp != "O2") return "Error: Object singleton O2 should have acAbstractProp property set to 'O2'";
    if (O2.foo() != 10) return "Error: Object singleton O2's foo() method should return 10";
    if (O3.foo() != 10) return "Error: Object singleton O3's foo() method should return 10";
    if (typeof new O3.SomeNestedClass !== "object") return "Error: O3.SomeNestedClass should be instantiable as an object"

    if (getI3().foo != "fooI3") return "Error: getI3() should return an object with foo property set to 'fooI3'"
    if (getI3().bar != "barI3") return "Error: getI3() should return an object with bar property set to 'barI3'"
    if (getI3().baz != "bazI3") return "Error: getI3() should return an object with baz property set to 'bazI3'"
    if (getI3().bay() != "bayI3") return "Error: getI3() should return an object with bay() method that returns 'bayI3'"

    if (getA().foo != "fooA") return "Error: getA() should return an object with foo property set to 'fooA'"
    if (getA().bar != "barA") return "Error: getA() should return an object with bar property set to 'barA'"
    if (getA().baz != "bazA") return "Error: getA() should return an object with baz property set to 'bazA'"
    if (getA().bay() != "bayA") return "Error: getA() should return an object with bay() method that returns 'bayA'"

    if (getB().foo != "fooB") return "Error: getB() should return an object with foo property set to 'fooB'"
    if (getB().bar != "barB") return "Error: getB() should return an object with bar property set to 'barB'"
    if (getB().baz != "bazB") return "Error: getB() should return an object with baz property set to 'bazB'"
    if (getB().bay() != "bayB") return "Error: getB() should return an object with bay() method that returns 'bayB'"

    if (getC().foo != "fooC") return "Error: getC() should return an object with foo property set to 'fooC'"
    if (getC().bar != "barC") return "Error: getC() should return an object with bar property set to 'barC'"
    if (getC().baz != "bazC") return "Error: getC() should return an object with baz property set to 'bazC'"
    if (getC().bay() != "bayC") return "Error: getC() should return an object with bay() method that returns 'bayC'"

    const a2Impl = new A2Impl()
    if (a2Impl.foo != "fooA2") return "Error: A2Impl instance's foo property should be 'fooA2'"
    if (a2Impl.bar != "barA2") return "Error: A2Impl instance's bar property should be 'barA2'"
    a2Impl.bar = "barA2.2"
    if (a2Impl.bar != "barA2.2") return "Error: A2Impl instance's bar property should be mutable and update to 'barA2.2'"
    if (a2Impl.baz != "bazA2") return "Error: A2Impl instance's baz property should be 'bazA2'"
    if (a2Impl.bay() != "bayA2") return "Error: A2Impl instance's bay() method should return 'bayA2'"

    const b2 = new B2()
    if (b2.foo != "fooB2") return "Error: B2 instance's foo property should be 'fooB2'"
    if (b2.bar != "barB2") return "Error: B2 instance's bar property should be 'barB2'"
    if (b2.baz != "bazB2") return "Error: B2 instance's baz property should be 'bazB2'"
    if (b2.bay() != "bayB2") return "Error: B2 instance's bay() method should return 'bayB2'"

    const c2 = new C2()
    if (c2.foo != "fooC2") return "Error: C2 instance's foo property should be 'fooC2'"
    if (c2.bar != "barC2") return "Error: C2 instance's bar property should be 'barC2'"
    if (c2.baz != "bazC2") return "Error: C2 instance's baz property should be 'bazC2'"
    c2.baz = "bazC2-2"
    if (c2.baz != "bazC2-2") return "Error: C2 instance's baz property should be mutable and update to 'bazC2-2'"
    if (c2.bay() != "bayC2") return "Error: C2 instance's bay() method should return 'bayC2'"

    if (EC.EC1.foo != "foo") return "Error: EC.EC1 enum entry's foo property should be 'foo'"
    if (EC.EC1.bar != "bar") return "Error: EC.EC1 enum entry's bar property should be 'bar'"
    if (EC.EC1.baz != "ec1") return "Error: EC.EC1 enum entry's baz property should be 'ec1'"
    if (EC.EC1.bay() != "bay") return "Error: EC.EC1 enum entry's bay() method should return 'bay'"

    if (EC.EC2.foo != "foo") return "Error: EC.EC2 enum entry's foo property should be 'foo'"
    if (EC.EC2.bar != "bar") return "Error: EC.EC2 enum entry's bar property should be 'bar'"
    if (EC.EC2.baz != "ec2") return "Error: EC.EC2 enum entry's baz property should be 'ec2'"
    if (EC.EC2.bay() != "bay") return "Error: EC.EC2 enum entry's bay() method should return 'bay'"

    if (EC.EC3.foo != "foo") return "Error: EC.EC3 enum entry's foo property should be 'foo'"
    if (EC.EC3.bar != "bar") return "Error: EC.EC3 enum entry's bar property should be 'bar'"
    if (EC.EC3.baz != "ec3") return "Error: EC.EC3 enum entry's baz property should be 'ec3'"
    if (EC.EC3.bay() != "bay") return "Error: EC.EC3 enum entry's bay() method should return 'bay'"

    return "OK";
}
