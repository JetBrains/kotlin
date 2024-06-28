// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE

// TODO fix statics export in DCE-driven mode
// SKIP_DCE_DRIVEN

// MODULE: JS_TESTS
// FILE: f1.kt

@JsExport
class ClassWithDefaultCtor {
    val x = "ClassWithDefaultCtor::x"
}

@JsExport
class ClassWithPrimaryCtor(
    val x: String
)

@JsExport
class ClassWithSecondaryCtor {
    val x: String
    @JsName("create")
    constructor(y: String) {
        x = y
    }
}

@JsExport
class ClassWithMultipleSecondaryCtors {
    val x: String

    @JsName("createFromString")
    constructor(y: String) {
        x = "fromString:$y"
    }

    @JsName("createFromInts")
    constructor(y: Int, z: Int) {
        x = "fromInts:$y:$z"
    }
}

@JsExport
open class OpenClassWithMixedConstructors(val x: String) {
    @JsName("createFromStrings")
    constructor(y: String, z: String) : this("fromStrings:$y:$z")

    @JsName("createFromInts")
    constructor(y: Int, z: Int) : this(y.toString(), z.toString())
}

@JsExport
class DerivedClassWithSecondaryCtor : OpenClassWithMixedConstructors {
    @JsName("delegateToPrimary")
    constructor(y: String) : super(y)

    @JsName("delegateToCreateFromInts")
    constructor(y: Int, z: Int) : super(y, z)
}

// FILE: f2.kt

@JsExport
class KotlinGreeter(val greeting: String = "helau")