// EXPECTED_REACHABLE_NODES: 1265
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_MINIFICATION
// SKIP_NODE_JS
// SKIP_DCE_DRIVEN

// MODULE: exportProtectedMembers
// FILE: lib.kt

@JsExport
open class Foo protected constructor() {
    protected fun bar(): String = "protected method"

    private var _baz: String = "baz"

    protected var baz: String
        get() = _baz
        set(value) {
            _baz = value
        }

    protected val bazReadOnly: String
        get() = _baz

    protected val quux: String = "quux"

    protected var quuz: String = "quuz"

    protected class NestedClass {
        val prop: String = "nested class property"
    }
    protected object NestedObject {
        val prop: String = "nested object property"
    }

    protected companion object {
        val prop: String = "companion object property"
    }
}

// FILE: test.js
function box() {
    foo = new exportProtectedMembers.Foo();

    if (foo.bar() != 'protected method') return 'failed to call protected method';
    if (foo.baz != 'baz') return 'failed to read `baz`';
    if (foo.bazReadOnly != 'baz') return 'failed to read `bazReadOnly`';
    foo.baz = 'beer';
    if (foo.baz != 'beer') return 'failed to write protected var';
    if (foo.bazReadOnly != 'beer') return 'unexpected value of `bazReadOnly` after modifying `baz`';
    if (foo.quux != 'quux') return 'failed to read `quux`';
    if (foo.quuz != 'quuz') return 'failed to read `quuz`';
    foo.quuz = 'ale';
    if (foo.quuz != 'ale') return 'failed to write `quuz`';

    nestedClass = new exportProtectedMembers.Foo.NestedClass()
    if (nestedClass.prop != 'nested class property')
        return 'failed to read protected class property'
    if (exportProtectedMembers.Foo.NestedObject.prop != 'nested object property')
        return 'failed to read protected nested object property'
    if (exportProtectedMembers.Foo.Companion.prop != 'companion object property')
        return 'failed to read protected companion object property'

    return 'OK';
}
