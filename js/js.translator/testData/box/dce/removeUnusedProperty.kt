// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: remove_unused_property
// FILE: lib.kt

external fun someEffectfulFunction(): Int

object B {
    var value = 5
}

fun withIndirectEffects(): Int {
    someEffectfulFunction()
    return 5
}

fun withGlobalMutation(): Int {
    B.value += 6
    return 512
}

class C(var v: Int = 42)

fun withParameterMutation(c: C): Int {
    c.v += 128
    return 256
}

fun withLocalMutation(): Int {
    var x = 5
    x += 6
    return 128
}

fun empty(): Int = 1024

class A {
    var x = 1
    var y = 42
    var z = someEffectfulFunction()
    var w = 16
    var a = withGlobalMutation()
    var b = withParameterMutation(C())
    var c = withLocalMutation()
    var d = empty()
}

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
    if (effectCount !== 2)
        return `expected effectCount to be exactly 4, have ${effectCount}`;
    // const ctor = a.prototype.constructor.toString();
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
