// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: remove_unused_property
// FILE: lib.kt

external fun someEffectfulFunction(): Int

fun withIndirectEffects(): Int {
    someEffectfulFunction()
    return 5
}

class A {
    var x = 1
    var y = 42
    var z = someEffectfulFunction()
    var w = 16
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
    if (ps.length != 1)
        return `expected to only have one field left (have ${ps.length}: ${ps})`;
    if (a[ps[0]] != 3)
        return `expected the 'x' field to have the value 3, have ${a[ps[0]]}`;
    if (effectCount != 2)
        return `expected someEffectfulFunction to be called exactly 2 times, have ${effectCount}`
    return "OK";
}
