// ERROR: Type inference failed: Cannot infer type parameter K in inline operator fun <K, V> MutableMap<K, V>.set(key: K, value: V): Unit None of the following substitutions receiver: MutableMap<CapturedTypeConstructor(out (kotlin.Any..kotlin.Any?)), Any?>  arguments: (CapturedTypeConstructor(out (kotlin.Any..kotlin.Any?)),Any?) receiver: MutableMap<Int, Any?>  arguments: (Int,Any?) can be applied to receiver: HashMap<*, *>  arguments: (Int,Int) 
// ERROR: Type argument is not within its bounds: should be subtype of 'String?'
import java.util.HashMap

internal class G<T : String?>(t: T)
class Java {
    fun test() {
        val m: HashMap<*, *> = HashMap<Any?, Any?>()
        m[1] = 1
    }

    fun test2() {
        val m: HashMap<*, *> = HashMap<Any?, Any?>()
        val g: G<*> = G<Any?>("")
        val g2 = G("")
    }
}