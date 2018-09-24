import java.io.Serializable
import java.util.*

interface Intf<I1, I2 : Serializable>
interface Intf2<out T : List<String>, M : T>
interface OtherIntf<O : CharSequence>
open class BaseClass<B : Any>
class MyClass<M1, M2> : Intf<Any, java.util.Date>, OtherIntf<String>, BaseClass<RuntimeException>() {
    val fld: List<Map<String, M1>>? = null
}

interface ABC {
    fun <T : CharSequence> abc(item: T, items: List<T>, vararg otherItems: T): List<T>
    fun <X> bcd(vararg a: Char): Int
}