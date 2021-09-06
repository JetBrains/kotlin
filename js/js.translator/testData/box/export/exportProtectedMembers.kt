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
    if (foo.baz != 'baz') return 'failed to read protected property';
    foo.baz = 'beer';
    if (foo.baz != 'beer') return 'failed to write protected property';

    nestedClass = new exportProtectedMembers.Foo.NestedClass()
    if (nestedClass.prop != 'nested class property')
        return 'failed to read protected class property'
    if (exportProtectedMembers.Foo.NestedObject.prop != 'nested object property')
        return 'failed to read protected nested object property'
    if (exportProtectedMembers.Foo.Companion.prop != 'companion object property')
        return 'failed to read protected companion object property'

    return 'OK';
}