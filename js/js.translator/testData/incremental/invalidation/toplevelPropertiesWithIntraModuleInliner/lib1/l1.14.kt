val globalVal = 4

inline val inlineGlobalVal: Int
    get() = 4

var globalVar = "1"

var globalVarBacking = 2
inline var inlineGlobalVar: String
    get() = (1 + globalVarBacking).toString()
    set(value) { globalVarBacking += value.toInt() }
