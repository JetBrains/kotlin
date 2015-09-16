import java.lang.reflect.Constructor

internal object X {
    @Throws(Exception::class)
    fun <T> foo(constructor: Constructor<T>, args1: Array<Any>, args2: Array<Any>) {
        constructor.newInstance(*args1)
        constructor.newInstance(args1, args2)
    }
}