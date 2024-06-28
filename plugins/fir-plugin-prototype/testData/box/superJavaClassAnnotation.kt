// FILE: Base_ShouldBeOpen.java
import org.jetbrains.kotlin.fir.plugin.AllOpen2;

@AllOpen2
public abstract class Base_ShouldBeOpen {
    public void baseMethod() {}
}

// FILE: Intf.java
import org.jetbrains.kotlin.fir.plugin.AllOpen2;

@AllOpen2
public interface Intf {
    void intfMethod();
}

// FILE: main.kt
open class BaseImpl : Base_ShouldBeOpen() {
    fun baseImplMethod_ShouldBeOpen() {}
}

class BaseImpl2_ShouldBeOpen : BaseImpl() {
    fun baseImpl2Method_ShouldBeOpen() {}
    val baseImpl2Property_ShouldBeOpen = ""
}

open class IntfImpl : Intf {
    override fun intfMethod() {}
    fun intfImplMethod_ShouldBeOpen() {}
}

class IntfImpl2_ShouldBeOpen : IntfImpl() {
    fun intfImpl2Method_ShouldBeOpen() {}
}

fun box() = "OK"
