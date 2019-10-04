import javaApi.JavaClass
import kotlinApi.KotlinClass
import java.util.HashMap

internal class X {
    operator fun get(index: Int): Int {
        return 0
    }
}

internal class C {
    fun foo(map: HashMap<String?, String?>): String? {
        return map["a"]
    }

    fun foo(x: X): Int {
        return x[0]
    }

    fun foo(kotlinClass: KotlinClass): Int {
        return kotlinClass.get(0) // not operator!
    }

    fun foo(javaClass: JavaClass): Int {
        return javaClass[0]
    }
}