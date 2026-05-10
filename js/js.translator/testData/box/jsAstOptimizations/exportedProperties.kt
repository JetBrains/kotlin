@JsExport
open class Parent {
    open val foo: UInt? = null
}

@JsExport
// EXPECT_GENERATED_JS: class=Child expect=exportedProperties.out.es6.js TARGET_BACKENDS=JS_IR_ES6
// EXPECT_GENERATED_JS: function=Child expect=exportedProperties.out.js TARGET_BACKENDS=JS_IR
class Child : Parent() {
    override val foo = null
}

fun box(): String {
    return "OK"
}
