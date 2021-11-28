// EXPECTED_REACHABLE_NODES: 1265
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_MINIFICATION
// SKIP_DCE_DRIVEN
// SKIP_NODE_JS

// See KT-43783

// MODULE: nestedObjectExport
// FILE: lib.kt

@JsExport
class Abc {
    companion object AbcCompanion {
        fun xyz(): String = "Companion object method OK"

        val prop: String
            get() = "Companion object property OK"
    }
}

@JsExport
class Foo {
    companion object {
        fun xyz(): String = "Companion object method OK"

        val prop: String
            get() = "Companion object property OK"
    }
}

@JsExport
sealed class MyEnum(val name: String) {
    object A: MyEnum("A")
    object B: MyEnum("B")
    object C: MyEnum("C")
}

@JsExport
object MyObject {
    object A {
        fun valueA() = "OK"
    }
    object B {
        fun valueB() = "OK"
    }
    object C {
        fun valueC() = "OK"
    }
}

// FILE: test.js

function box() {
    const abcCompanion = nestedObjectExport.Abc.AbcCompanion;

    if (abcCompanion.xyz() != 'Companion object method OK') return 'companion object function failure';
    if (abcCompanion.prop != 'Companion object property OK') return 'companion object property failure';

    const justCompanion = nestedObjectExport.Foo.Companion;

    if (justCompanion.xyz() != 'Companion object method OK') return 'companion object function failure';
    if (justCompanion.prop != 'Companion object property OK') return 'companion object property failure';

    if (nestedObjectExport.MyEnum.A.name != 'A') return 'MyEnum.A failure';
    if (nestedObjectExport.MyEnum.B.name != 'B') return 'MyEnum.B failure';
    if (nestedObjectExport.MyEnum.C.name != 'C') return 'MyEnum.C failure';

    if (nestedObjectExport.MyObject.A.valueA() != "OK") return 'MyObject.A failure';
    if (nestedObjectExport.MyObject.B.valueB() != "OK") return 'MyObject.B failure';
    if (nestedObjectExport.MyObject.C.valueC() != "OK") return 'MyObject.C failure';

    return 'OK';
}
