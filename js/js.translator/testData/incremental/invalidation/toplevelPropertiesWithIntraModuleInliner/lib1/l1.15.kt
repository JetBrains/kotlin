val globalVal = 4

inline val inlineGlobalVal: Int
    get() = 4

var globalVar = "1"

var globalVarBacking = 2
inline var inlineGlobalVar: String
    get() = (1 + globalVarBacking).toString()
    set(value) { globalVarBacking += 1 + value.toInt() }

var globalExtensionPropertyAdd = 1
inline var Int.inlineExtensionProperty: String
    get() = (this + globalExtensionPropertyAdd).toString()
    set(value) { globalExtensionPropertyAdd += value.toInt() }
