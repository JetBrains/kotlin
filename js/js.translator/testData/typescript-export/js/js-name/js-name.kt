// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: js-name.kt

package foo

@JsExport
@JsName("Object")
external interface WeirdInterface {
    val constructor: dynamic
}

@JsExport
@JsName("JsNameTest")
class __JsNameTest internal constructor() {
    val withGetter1: String
        @JsName("testName1")
        get() = "name1"

    @get:JsName("testName2")
    val withGetter2: String get() = "name2"

    var withSetter1: String = ""
        @JsName("getWithSetter1")
        get() = "name1"
        @JsName("setWithSetter1")
        set(value) { field = value }

    @get:JsName("getWithSetter2")
    @set:JsName("setWithSetter2")
    var withSetter2: String = "name2"

    @JsName("value")
    val __value = 4

    @JsName("runTest")
    fun __runTest(): String {
        return "JsNameTest"
    }

    @JsName("acceptObject")
    fun __acceptWeirdImpl(impl: WeirdInterface): String {
        return impl.constructor.name
    }

    @JsName("NotCompanion")
    companion object {
        @JsName("create")
        fun __create(): __JsNameTest {
           return __JsNameTest()
        }

        @JsName("createChild")
        fun __createChild(value: Int): __NestJsNameTest {
           return  __NestJsNameTest(value)
        }
    }

    @JsName("NestedJsName")
    class __NestJsNameTest(@JsName("value") val __value: Int)
}

@JsExport
@JsName("TestInterface")
interface __TestInterface {
    val withGetter1: String
        @JsName("testName1") get() = ""

    @get:JsName("testName2")
    val withGetter2: String

    var withSetter1: String
        @JsName("getWithSetter1")
        get() = ""
        @JsName("setWithSetter1")
        set(value) {}

    @get:JsName("getWithSetter2")
    @set:JsName("setWithSetter2")
    var withSetter2: String

    @JsName("value")
    val __value: Int

    @JsName("runTest")
    fun __runTest(): String

    @JsName("acceptObject")
    fun __acceptWeirdImpl(impl: WeirdInterface): String

    @JsName("NotCompanion")
    companion object {
        @JsName("create")
        fun __create(): __JsNameTest {
            return __JsNameTest()
        }

        @JsName("createChild")
        fun __createChild(value: Int): __NestJsNameTest {
            return  __NestJsNameTest(value)
        }
    }

    @JsName("NestedJsName")
    class __NestJsNameTest(@JsName("value") val __value: Int)
}
