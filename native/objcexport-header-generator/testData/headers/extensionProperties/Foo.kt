val topLevelPropA = 0
val topLevelPropB = 1

val Foo.extensionValA
    get() = 0
val Foo.extensionValB
    get() = 1

var Foo.extensionVarA
    get() = 0
    set(value) {}
var Foo.extensionVarB
    get() = 1
    set(value) {}

class Foo {
    fun memberFun() {}
}