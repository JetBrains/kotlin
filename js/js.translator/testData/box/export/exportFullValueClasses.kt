// LANGUAGE: +FullValueClasses
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: lib
// FILE: lib.kt
@JsExport
value class SingleField(val x: Int)

@JsExport
value class MultiField(val x: Int, val y: Int)

@JsExport
abstract value class AbstractValueClass

@JsExport
sealed value class SealedValueClass

@JsExport
value class WithValueSuperClass(val x: Int) : AbstractValueClass()

@JsExport
value class WithSealedValueSuperClass(val x: Int) : SealedValueClass()

@JsExport
value object ValueObject

@JsExport
fun singleFieldX(value: SingleField): Int = value.x

@JsExport
fun makeSingleField(x: Int): SingleField = SingleField(x)

@JsExport
fun multiFieldSum(value: MultiField): Int = value.x + value.y

@JsExport
fun makeMultiField(x: Int, y: Int): MultiField = MultiField(x, y)

@JsExport
fun areEqual(a: MultiField, b: MultiField): Boolean = a == b

// FILE: test.js
function box() {
    var lib = this.lib;

    if (new lib.SingleField(1).x !== 1) return "Fail: SingleField";
    if (lib.singleFieldX(new lib.SingleField(2)) !== 2) return "Fail: singleFieldX";
    if (lib.makeSingleField(3).x !== 3) return "Fail: makeSingleField";

    var multiField = new lib.MultiField(4, 5);
    if (multiField.x !== 4 || multiField.y !== 5) return "Fail: MultiField";
    if (lib.multiFieldSum(multiField) !== 9) return "Fail: multiFieldSum";
    if (lib.makeMultiField(6, 7).y !== 7) return "Fail: makeMultiField";
    if (!lib.areEqual(new lib.MultiField(8, 9), new lib.MultiField(8, 9))) return "Fail: areEqual";

    var withValueSuperClass = new lib.WithValueSuperClass(10);
    if (withValueSuperClass.x !== 10) return "Fail: WithValueSuperClass";
    if (!(withValueSuperClass instanceof lib.AbstractValueClass)) return "Fail: instanceof AbstractValueClass";

    var withSealedValueSuperClass = new lib.WithSealedValueSuperClass(11);
    if (withSealedValueSuperClass.x !== 11) return "Fail: WithSealedValueSuperClass";
    if (!(withSealedValueSuperClass instanceof lib.SealedValueClass)) return "Fail: instanceof SealedValueClass";

    if (lib.ValueObject == null) return "Fail: ValueObject";

    return "OK";
}
