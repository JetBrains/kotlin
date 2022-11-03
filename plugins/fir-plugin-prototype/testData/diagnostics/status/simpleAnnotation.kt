// FIR_DISABLE_LAZY_RESOLVE_CHECKS
import org.jetbrains.kotlin.fir.plugin.AllOpen

@AllOpen
class A {
    fun foo() {

    }
}

@AllOpen
class B : A() {
    override fun foo() {

    }
}