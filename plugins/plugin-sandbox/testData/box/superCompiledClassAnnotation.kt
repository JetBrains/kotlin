// MODULE: lib
// FILE: lib.kt
import org.jetbrains.kotlin.fir.plugin.AllOpen2

@AllOpen2
interface Intf {
    fun intfMethod() {}
}

// FILE: Base_ShouldBeOpen.java
import org.jetbrains.kotlin.fir.plugin.AllOpen2;

@AllOpen2
public abstract class Base_ShouldBeOpen {
    public void baseMethod() {}
}

// MODULE: main(lib)
open class BaseImpl : Base_ShouldBeOpen() {
    fun baseImplMethod_ShouldBeOpen() {}
}

class BaseImpl2_ShouldBeOpen : BaseImpl() {
    fun baseImpl2Method_ShouldBeOpen() {}
    val baseImpl2Property_ShouldBeOpen = ""
}

open class IntfImpl : Intf {
    fun intfImplMethod_ShouldBeOpen() {}
}

class IntfImpl2_ShouldBeOpen : IntfImpl() {
    fun intfImpl2Method_ShouldBeOpen() {}
}

fun box() = "OK"
