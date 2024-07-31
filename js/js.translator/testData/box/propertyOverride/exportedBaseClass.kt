// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_MINIFICATION
// SKIP_DCE_DRIVEN
// SKIP_NODE_JS

// MODULE: exportedBaseClass
// FILE: lib.kt
@JsExport
abstract class ExportedBase {
    abstract val foo: String
    abstract var bar: String
    abstract val baz: String

    val fooFinal: String
        get() = foo
}

@JsExport
open class ExportedDerived1 : ExportedBase() {
    override val foo: String
        get() = "ExportedDerived1.foo"

    private var _bar = "ExportedDerived1.bar"

    override var bar: String
        get() = _bar
        set(value) { _bar = value }

    private var _baz = "ExportedDerived1.baz"

    override var baz: String
        get() = _baz
        set(value) { _baz = value }
}

@JsExport
class ExportedDerived2 : ExportedDerived1() {
    override val foo: String
        get() = "ExportedDerived2.foo"

    private var _bar = "ExportedDerived2.bar"

    override var bar: String
        get() = _bar
        set(value) { _bar = value }

    private var _baz = "ExportedDerived2.baz"

    override var baz: String
        get() = _baz
        set(value) { _baz = value }
}

abstract class NonExportedBase {
    abstract val foo: String
    abstract var bar: String
    abstract val baz: String
}

@JsExport
class ExportedDerived3 : NonExportedBase() {
    override val foo: String
        get() = "ExportedDerived3.foo"

    private var _bar = "ExportedDerived3.bar"

    override var bar: String
        get() = _bar
        set(value) { _bar = value }

    private var _baz = "ExportedDerived3.baz"

    override var baz: String
        get() = _baz
        set(value) { _baz = value }
}

// Non-exported
open class Derived1 : ExportedBase() {
    override val foo: String
        get() = "1"

    private var _bar = "1"

    override var bar: String
        get() = _bar
        set(value) { _bar = value }

    private var _baz = "11"

    override var baz: String
        get() = _baz
        set(value) { _baz = value }
}

class Derived2 : Derived1() {
    override val foo: String
        get() = "2"

    private var _bar = "2"

    override var bar: String
        get() = _bar
        set(value) { _bar = value }

    private var _baz = "22"

    override var baz: String
        get() = _baz
        set(value) { _baz = value }
}

@JsExport
fun getDerived1(): ExportedBase = Derived1()

@JsExport
fun getDerived2(): ExportedBase = Derived2()

// FILE: test.js
function assertEquals(expected, actual, msg) {
    if (expected !== actual) {
        throw "Unexpected value: expected = '" + expected + "', actual = '" + actual + "' — " + msg;
    }
}

function box() {

    var d = exportedBaseClass;

    assertEquals(
        true,
        d.ExportedBase.prototype.hasOwnProperty('foo'),
        'Property foo should be defined for ExportedBase.prototype'
    );
    assertEquals(
        true,
        d.ExportedBase.prototype.hasOwnProperty('bar'),
        'Property bar should be defined for ExportedBase.prototype'
    );
    assertEquals(
        true,
        d.ExportedBase.prototype.hasOwnProperty('baz'),
        'Property baz should be defined for ExportedBase.prototype'
    );

    assertEquals(
        true,
        d.ExportedBase.prototype.hasOwnProperty('fooFinal'),
        'Property fooFinal should be defined for ExportedBase.prototype'
    );

    var derived1 = d.getDerived1();
    assertEquals('1', derived1.foo, "derived1.foo");
    assertEquals('1', derived1.fooFinal, "derived1.fooFinal");
    assertEquals('1', derived1.bar, "derived1.bar initial value");
    derived1.bar = '11';
    assertEquals('11', derived1.bar, "derived1.bar after write");
    assertEquals('11', derived1.baz, "derived1.baz initial value");
    assertEquals(
        false,
        Object.getPrototypeOf(derived1).hasOwnProperty('foo'),
        'Property foo of Derived1 should be inherited from ExportedBase.prototype'
    );
    assertEquals(
        false,
        Object.getPrototypeOf(derived1).hasOwnProperty('bar'),
        'Property bar should of Derived1 be inherited from ExportedBase.prototype'
    );
    assertEquals(
        false,
        Object.getPrototypeOf(derived1).hasOwnProperty('baz'),
        'Property baz should of Derived1 be inherited from ExportedBase.prototype'
    );

    var derived2 = d.getDerived2();
    assertEquals('2', derived2.foo, "derived2.foo");
    assertEquals('2', derived2.fooFinal, "derived2.fooFinal");
    assertEquals('2', derived2.bar, "derived2.bar initial value");
    derived2.bar = '22';
    assertEquals('22', derived2.bar, "derived2.bar after write");
    assertEquals('22', derived2.baz, "derived2.baz initial value");
    assertEquals(
        false,
        Object.getPrototypeOf(derived2).hasOwnProperty('foo'),
        'Property foo of Derived2 should be inherited from ExportedBase.prototype'
    );
    assertEquals(
        false,
        Object.getPrototypeOf(derived2).hasOwnProperty('bar'),
        'Property bar of Derived2 should be inherited from ExportedBase.prototype'
    );
    assertEquals(
        false,
        Object.getPrototypeOf(derived2).hasOwnProperty('baz'),
        'Property baz of Derived2 should be inherited from ExportedBase.prototype'
    );

    var exportedDerived1 = new d.ExportedDerived1();
    assertEquals('ExportedDerived1.foo', exportedDerived1.foo, "exportedDerived1.foo");
    assertEquals('ExportedDerived1.bar', exportedDerived1.bar, "exportedDerived1.bar initial value");
    exportedDerived1.bar = 'ExportedDerived1.bar (updated)';
    assertEquals('ExportedDerived1.bar (updated)', exportedDerived1.bar, "exportedDerived1.bar after write");
    assertEquals('ExportedDerived1.baz', exportedDerived1.baz, "exportedDerived1.baz initial value");
    exportedDerived1.baz = 'ExportedDerived1.baz (updated)';
    assertEquals('ExportedDerived1.baz (updated)', exportedDerived1.baz, "exportedDerived1.baz after write");
    assertEquals(
        false,
        d.ExportedDerived1.prototype.hasOwnProperty('foo'),
        'Property foo of ExportedDerived1 should be inherited from ExportedBase.prototype'
    );
    assertEquals(
        false,
        d.ExportedDerived1.prototype.hasOwnProperty('bar'),
        'Property bar of ExportedDerived1 should be inherited from ExportedBase.prototype'
    );

    // true because it is val -> var override, to we need to redefine property with setter
    assertEquals(
        true,
        d.ExportedDerived1.prototype.hasOwnProperty('baz'),
        'Property baz of ExportedDerived1 should be defined for ExportedDerived1.prototype'
    );

    var exportedDerived2 = new d.ExportedDerived2();
    assertEquals(
        false,
        d.ExportedDerived2.prototype.hasOwnProperty('foo'),
        'Property foo should not be defined for ExportedDerived2.prototype'
    );
    assertEquals(
        false,
        d.ExportedDerived2.prototype.hasOwnProperty('bar'),
        'Property bar should not be defined for ExportedDerived2.prototype'
    );
    assertEquals(
        false,
        d.ExportedDerived2.prototype.hasOwnProperty('baz'),
        'Property baz should not be defined for ExportedDerived2.prototype'
    );

    var exportedDerived3 = new d.ExportedDerived3 ();
    assertEquals(
        false,
        d.ExportedDerived3.prototype.hasOwnProperty('foo'),
        'Property foo should not be defined for ExportedDerived3.prototype'
    );
    assertEquals(
        false,
        d.ExportedDerived3.prototype.hasOwnProperty('bar'),
        'Property bar should not be defined for ExportedDerived3.prototype'
    );
    assertEquals(
        false,
        d.ExportedDerived3.prototype.hasOwnProperty('baz'),
        'Property baz should not be defined for ExportedDerived3.prototype'
    );

    return 'OK';
}