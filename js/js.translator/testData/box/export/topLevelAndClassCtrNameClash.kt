// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// K1 reports a name clash error between top-level declarations and constructors. It seems relevant for legacy JS BE;
// however, this is not applicable to JS IR BE because in the generated JS code, there is no name clash.
// K2 doesn't report anything and works correctly with JS IR BE.

// RUN_PLAIN_BOX_FUNCTION
// MODULE: lib
// FILE: lib.kt
@JsExport
class ClassA {
    val a: String

    @JsName("createFromString")
    constructor(y: String) {
        a = "ClassA:fromString:$y"
    }

    @JsName("createFromInt")
    constructor(y: Int) {
        a = "ClassA:fromInt:$y"
    }
}

@JsExport
class ClassB {
    val b: String

    @JsName("createFromString")
    constructor(y: String) {
        b = "ClassB:fromString:$y"
    }

    @JsName("createFromInt")
    constructor(y: Int) {
        b = "ClassB:fromInt:$y"
    }
}

@JsExport
fun createFromString(y: String) = "fromString:$y";

@JsExport
fun createFromInt(y: Int) = "fromInt:$y";

// FILE: main.js
function box() {
    var a1 = this.lib.ClassA.createFromString("A1");
    var a2 = this.lib.ClassA.createFromInt(2);
    var b3 = this.lib.ClassB.createFromString("B3");
    var b4 = this.lib.ClassB.createFromInt(4);
    var x5 = this.lib.createFromString("X5");
    var x6 = this.lib.createFromInt(6);

    if (a1.a !== "ClassA:fromString:A1") return "Error: '" + a1.a + "' !== 'ClassA:fromString:A1'"
    if (a2.a !== "ClassA:fromInt:2") return "Error: '" + a2.a + "' !== 'ClassA:fromInt:2'"
    if (b3.b !== "ClassB:fromString:B3") return "Error: '" + b3.b + "' !== 'ClassB:fromString:B3'"
    if (b4.b !== "ClassB:fromInt:4") return "Error: '" + b4.b + "' !== 'ClassB:fromInt:4'"
    if (x5 !== "fromString:X5") return "Error: '" + x5 + "' !== 'fromString:X5'"
    if (x6 !== "fromInt:6") return "Error: '" + x6 + "' !== 'fromInt:6'"

    return "OK"
}
