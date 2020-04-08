package fwPropertyInInterface

// Breakpoint at getter/setter
interface MyInterface {
    //FieldWatchpoint! (propVal)
    val propVal: Int

    //FieldWatchpoint! (propVar)
    var propVar: Int

    fun testPropertyInInterface() {
        propVal
        propVar
        propVar = 2
    }
}

class MyInterfaceImpl : MyInterface {
    override val propVal = 1
    override var propVar = 1

    fun testPropertyInInterfaceImpl() {
        propVal
        propVar
        propVar = 2
    }
}

interface MyInterfaceWithoutBreakpoints {
    val propVal2: Int
    var propVar2: Int

    fun testPropertyInInterface() {
        propVal2
        propVar2
        propVar2 = 2
    }
}

// Breakpoint at GETFILED/PUTFIELD
class MyInterfaceImplWithBreakpoints : MyInterfaceWithoutBreakpoints {
    //FieldWatchpoint! (propVal2)
    override val propVal2 = 1

    //FieldWatchpoint! (propVar2)
    override var propVar2 = 1

    fun testPropertyInInterfaceImpl() {
        propVal2
        propVar2
        propVar2 = 2
    }
}

fun main(args: Array<String>) {
    val mac = object: MyInterface {
        override val propVal: Int get() = 1
        override var propVar: Int
            get() = 1
            set(value) {
            }
    }
    mac.testPropertyInInterface()

    val maci = MyInterfaceImpl()
    maci.testPropertyInInterface()
    maci.testPropertyInInterfaceImpl()

    val macwb = object: MyInterfaceWithoutBreakpoints {
        override val propVal2: Int get() = 1
        override var propVar2: Int
            get() = 1
            set(value) {
            }
    }
    macwb.testPropertyInInterface()

    val macwbi = MyInterfaceImplWithBreakpoints()
    macwbi.testPropertyInInterface()
    macwbi.testPropertyInInterfaceImpl()
}

// RESUME: 17