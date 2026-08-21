// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: remove_unused_property
// FILE: lib.kt

external fun someEffectfulFunction(): Int

object B {
    var value = 5
}

object BwithEffects {
    var value = someEffectfulFunction()
}

// FUNCTION_HAS_EFFECTS: function=withIndirectEffects WRITE
fun withIndirectEffects(): Int {
    someEffectfulFunction()
    return 5
}

// FUNCTION_HAS_EFFECTS: function=withGlobalMutation WRITE
fun withGlobalMutation(): Int {
    B.value += 6
    return 512
}

class C(var v: Int = 42)

// FUNCTION_HAS_EFFECTS: function=withParameterMutation WRITE
fun withParameterMutation(c: C): Int {
    c.v += 128
    return 256
}

// FUNCTION_HAS_EFFECTS: function=withLocalMutation PURE
fun withLocalMutation(): Int {
    var x = 5
    x += 6
    return 128
}

// FUNCTION_HAS_EFFECTS: function=withGlobalRead READ
fun withGlobalRead(): Int {
    return B.value
}

// FUNCTION_HAS_EFFECTS: function=withGlobalReadButEffects WRITE
fun withGlobalReadButEffects(): Int {
    return BwithEffects.value
}

// FUNCTION_HAS_EFFECTS: function=empty PURE
fun empty(): Int = 1024

// FUNCTION_HAS_EFFECTS: constructor=A WRITE TARGET_BACKENDS=JS_IR_ES6
// FUNCTION_HAS_EFFECTS: function=A WRITE TARGET_BACKENDS=JS_IR
class A {
    var x = 1
    var y = 42
    var z = someEffectfulFunction()
    var w = 16
    var a = withGlobalMutation()
    var b = withParameterMutation(C())
    var c = withLocalMutation()
    var d = empty()
    var e = withGlobalRead()
    var f = withGlobalReadButEffects()
}

// FUNCTION_HAS_EFFECTS: function=createAndUse WRITE
@JsExport
fun createAndUse(): A {
    val a = A()
    a.x += 2
    a.w = withIndirectEffects()
    return a
}

// FILE: test.js
let effectCount = 0;

function someEffectfulFunction() {
    effectCount += 1;
    return 256;
}

function box() {
    const a = this["remove_unused_property"].createAndUse();

    // we should only have a single field left (x in kotlin)
    const ps = Object.getOwnPropertyNames(a);
    if (ps.length !== 1)
        return `expected to only have one field left (have ${ps.length}: ${ps})`;
    if (a[ps[0]] !== 3)
        return `expected the 'x' field to have the value 3, have ${a[ps[0]]}`;
    if (effectCount !== 3)
        return `expected effectCount to be exactly 3, have ${effectCount}`;
    const ctor = Object.getPrototypeOf(a).constructor.toString();
    if (!ctor.includes("withGlobalMutation("))
        return "expected A's constructor to invoke withGlobalMutation";
    if (!ctor.includes("withParameterMutation("))
        return "expected A's constructor to invoke withParameterMutation";
    if (ctor.includes("withLocalMutation("))
        return "expected A's constructor to not invoke withLocationMutation (it should be pure)";
    if (ctor.includes("empty("))
        return "expected A's constructor to not invoke empty (it should be pure)";
    return "OK";
}
