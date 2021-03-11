// KT-13485
package smartStepIntoInterfaceFun

interface ObjectFace<T> {
    fun act()
    fun act2(t: T)
}

class ObjectClass : ObjectFace<Int> {
    override fun act() {
        println()
    }
    override fun act2(t: Int) {
        println()
    }
}

fun main(args: Array<String>) {
    val simple: ObjectFace<Int> = ObjectClass()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    simple.act()
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    simple.act2(1)
}
