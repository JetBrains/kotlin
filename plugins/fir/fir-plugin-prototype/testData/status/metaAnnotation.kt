import org.jetbrains.kotlin.fir.plugin.AllOpen

@Open
class A {
    fun foo() {

    }
}

@Open
class B : A() {
    override fun foo() {

    }
}

@AllOpen
annotation class Open