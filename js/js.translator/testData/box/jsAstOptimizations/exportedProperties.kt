@JsExport
open class Parent {
    open val foo: UInt? = null
}

@JsExport
// EXPECT_GENERATED_JS: class=Child expect=exportedProperties.out.es6.js TARGET_BACKENDS=JS_IR_ES6 IGNORED_MODES=USE_CONST_AND_LET_FOR_VARIABLES
// EXPECT_GENERATED_JS: function=Child expect=exportedProperties.out.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: class=Child expect=exportedProperties.constLet.out.js TARGET_MODES=USE_CONST_AND_LET_FOR_VARIABLES
class Child : Parent() {
    override val foo = null
}

fun box(): String {
    return "OK"
}
