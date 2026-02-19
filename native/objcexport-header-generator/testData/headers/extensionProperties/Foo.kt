val topLevelPropA = 0
val topLevelPropB = 1

val Clazz.extensionValA
    get() = 0
val Clazz.extensionValB
    get() = 1

var Clazz.extensionVarA
    get() = 0
    set(value) {}
var Clazz.extensionVarB
    get() = 1
    set(value) {}

class Clazz {
    fun memberFun() {}
}