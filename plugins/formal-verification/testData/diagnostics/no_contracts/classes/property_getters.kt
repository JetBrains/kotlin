// NEVER_VALIDATE

class PrimitiveProperty {
    val nProp: Int get() {
        return 0
    }
}

fun <!VIPER_TEXT!>testPrimitivePropertyGetter<!>(pp: PrimitiveProperty) : Int = pp.nProp

class ReferenceProperty {
    val rProp: PrimitiveProperty get() {
        return PrimitiveProperty()
    }
}

fun <!VIPER_TEXT!>testReferencePropertyGetter<!>(rp: ReferenceProperty) {
    val pp = rp.rProp
    val ppn = pp.nProp
}

fun <!VIPER_TEXT!>testCascadingPropertyGetter<!>(rp: ReferenceProperty) {
    val ppn = rp.rProp.nProp
}