// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: inner-class-generic.kt

@file:JsExport


external interface Box<T, Self : Box<T, Self>> {
    fun unbox(): T
    fun copy(newValue: T): Self
}


class GenericTestInner<T : Box<String, T>>(val a: T) {
    inner class Inner(val a: T) {
        val concat: String = this@GenericTestInner.a.unbox() + this.a.unbox()

        @JsName("fromNumber")
        constructor(a: Int): this(this@GenericTestInner.a.copy(a.toString()))

        @JsName("SecondLayerInner")
        inner class InnerInner(val a: T) {
            val concat: String = this@GenericTestInner.a.unbox() + this@Inner.a.unbox() + this.a.unbox()
        }
    }

    inner class GenericInner<S : Box<R, S>, R : String>(val a: S) {
        val concat: String = this@GenericTestInner.a.unbox() + this.a.unbox()

        @JsName("fromNumber")
        constructor(a: Int, copier: S): this(copier.copy(a.toString() as R))

        @JsName("SecondLayerGenericInner")
        inner class GenericInnerInner<U : Box<String, U>, V>(val a: U, val v: V) {
            val concat: String = this@GenericTestInner.a.unbox() + this@GenericInner.a.unbox() + this.a.unbox()
        }
    }

    inner class GenericInnerWithShadowingTP<T : Box<String, T>>(val a: T) {
        val concat: String = this@GenericTestInner.a.unbox() + this.a.unbox()

        @JsName("fromNumber")
        constructor(a: Int, copier: T): this(copier.copy(a.toString()))
    }

    open inner class OpenInnerWithPublicConstructor(val a: T) {
        val concat: String = this@GenericTestInner.a.unbox() + this.a.unbox()

        @JsName("fromNumber")
        constructor(a: Int): this(this@GenericTestInner.a.copy(a.toString()))
    }

    inner class SubclassOfOpenInnerWithPublicConstructor(a: T) : OpenInnerWithPublicConstructor(a)

    open inner class GenericOpenInnerWithPublicConstructor<S : Box<String, S>>(val a: S) {
        val concat: String = this@GenericTestInner.a.unbox() + this.a.unbox()

        @JsName("fromNumber")
        constructor(a: Int, copier: S): this(copier.copy(a.toString()))
    }

    inner class SubclassOfGenericOpenInnerWithPublicConstructor(a: T) : GenericOpenInnerWithPublicConstructor<T>(a)
    inner class GenericSubclassOfGenericOpenInnerWithPublicConstructor1<S : Box<String, S>>(a: S) : GenericOpenInnerWithPublicConstructor<S>(a)

    open inner class OpenInnerWithProtectedConstructor protected constructor(val a: T) {
        val concat: String = this@GenericTestInner.a.unbox() + this.a.unbox()

        @JsName("fromNumber")
        protected constructor(a: Int): this(this@GenericTestInner.a.copy(a.toString()))
    }

    open inner class GenericOpenInnerWithProtectedConstructor<S : Box<String, S>> protected constructor(val a: S) {
        val concat: String = this@GenericTestInner.a.unbox() + this.a.unbox()

        @JsName("fromNumber")
        protected constructor(a: Int, copier: S): this(copier.copy(a.toString()))
    }
}