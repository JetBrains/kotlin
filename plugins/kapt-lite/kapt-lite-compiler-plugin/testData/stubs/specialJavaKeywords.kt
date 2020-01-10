// WITH_RUNTIME
import kotlin.jvm.Strictfp

class Foo {
    @Transient
    val tr: Int = 0

    @Volatile
    var vl: Int = 0

    @Strictfp
    fun sfp() {}

    @Synchronized
    fun sync() {}
}