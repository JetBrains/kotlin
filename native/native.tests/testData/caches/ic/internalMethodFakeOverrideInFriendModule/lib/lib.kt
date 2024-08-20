var DemoSetterCalls = 0
var DemoGetterCalls = 0

abstract class Demo {
    internal fun demoFun(): Int = 5
    internal val demoVal: Int = 6
    internal val demoValGet: Int
        get() = 7
    internal var demoVarSetGet: Int = 8
        set(value) { ++DemoSetterCalls; field = value }
        get() { ++DemoGetterCalls; return field }
}