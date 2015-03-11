import java.lang.reflect.Constructor

class X {
    default object {
        throws(javaClass<Exception>())
        fun <T> foo(constructor: Constructor<T>, args1: Array<Any>, args2: Array<Any>) {
            constructor.newInstance(*args1)
            constructor.newInstance(args1, args2)
        }
    }
}