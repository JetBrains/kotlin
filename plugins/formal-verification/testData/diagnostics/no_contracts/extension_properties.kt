// NEVER_VALIDATE

val Int.intValProp: Int get() = this

var Int.intVarProp: Int
    get() = 0
    set(v) {}

fun <!VIPER_TEXT!>extensionGetterProperty<!>() {
    val a = 0.intValProp
    val b = 1.intValProp.intValProp
}

fun <!VIPER_TEXT!>extensionSetterProperty<!>() {
    42.intVarProp = 0
}

class PrimitiveField(val x: Int)

val PrimitiveField.pfValProp: Int get() = this.x

var PrimitiveField.pfVarProp: Int
    get() = 0
    set(v) {}

fun <!VIPER_TEXT!>extensionGetterPropertyUserDefinedClass<!>(pf: PrimitiveField) {
    val x = pf.pfValProp
}

fun <!VIPER_TEXT!>extensionSetterPropertyUserDefinedClass<!>(pf: PrimitiveField) {
    pf.pfVarProp = 42
}