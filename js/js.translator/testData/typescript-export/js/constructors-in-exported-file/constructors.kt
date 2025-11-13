// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE

// TODO fix statics export in DCE-driven mode
// SKIP_DCE_DRIVEN

// MODULE: JS_TESTS
// FILE: f1.kt

@file:JsExport


class ClassWithDefaultCtor {
    val x = "ClassWithDefaultCtor::x"
}


class ClassWithPrimaryCtor(
    val x: String
)


class ClassWithSecondaryCtor {
    val x: String
    @JsName("create")
    constructor(y: String) {
        x = y
    }
}


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


open class OpenClassWithMixedConstructors(val x: String) {
    @JsName("createFromStrings")
    constructor(y: String, z: String) : this("fromStrings:$y:$z")

    @JsName("createFromInts")
    constructor(y: Int, z: Int) : this(y.toString(), z.toString())
}


class DerivedClassWithSecondaryCtor : OpenClassWithMixedConstructors {
    @JsName("delegateToPrimary")
    constructor(y: String) : super(y)

    @JsName("delegateToCreateFromInts")
    constructor(y: Int, z: Int) : super(y, z)
}


open class GenericClassWithSecondaryCtor<Self: GenericClassWithSecondaryCtor<Self>> {
    val x: String
    @JsName("createFromString")
    constructor(y: String) {
        x = y
    }
}

// FILE: f2.kt

@file:JsExport


class KotlinGreeter(val greeting: String = "helau")