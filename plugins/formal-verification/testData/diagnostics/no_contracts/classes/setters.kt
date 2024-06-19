// NEVER_VALIDATE

class PrimitiveField(var a: Int)

class ReferenceField(var pf: PrimitiveField)

fun <!VIPER_TEXT!>testPrimitiveFieldSetter<!>(pf: PrimitiveField) {
    pf.a = 0
}

fun <!VIPER_TEXT!>testReferenceFieldSetter<!>(rf: ReferenceField) {
    rf.pf = PrimitiveField(0)
}

class PrimitiveProperty {
    var aProp: Int = 0
        set(v) {}
}

class ReferenceProperty {
    var ppProp: PrimitiveProperty = PrimitiveProperty()
        set(v) {}
}

fun <!VIPER_TEXT!>testPrimitivePropertySetter<!>(pp: PrimitiveProperty) {
    pp.aProp = 0
}

fun <!VIPER_TEXT!>testReferencePropertySetter<!>(rp: ReferenceProperty) {
    rp.ppProp = PrimitiveProperty()
}
