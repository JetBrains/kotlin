annotation class AllOpen

@AllOpen
abstract class Base_ShouldBeOpen {
    fun baseMethod() {}
}

open class BaseImpl : Base_ShouldBeOpen() {
    fun baseImplMethod_ShouldBeOpen() {}
}

class BaseImpl2_ShouldBeOpen : BaseImpl() {
    fun baseImpl2Method_ShouldBeOpen() {}
    val baseImpl2Property_ShouldBeOpen = ""
}

@AllOpen
interface Intf {
    fun intfMethod() {}
}

open class IntfImpl : Intf {
    fun intfImplMethod_ShouldBeOpen() {}
}

class IntfImpl2_ShouldBeOpen : IntfImpl() {
    fun intfImpl2Method_ShouldBeOpen() {}
}