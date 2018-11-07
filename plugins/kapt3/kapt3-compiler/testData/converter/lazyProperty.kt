// WITH_RUNTIME

interface Intf
interface GenericIntf<T>

class Foo {
    private val foo by lazy {
        object : Runnable {
            override fun run() {}
        }
    }

    private val bar by lazy {
        object : Runnable, Intf {
            override fun run() {}
        }
    }

    private val baz by lazy {
        abstract class LocalIntf
        object : LocalIntf() {}
    }

    private val generic1 by lazy {
        abstract class LocalIntf : GenericIntf<CharSequence>
        object : LocalIntf() {}
    }
}
