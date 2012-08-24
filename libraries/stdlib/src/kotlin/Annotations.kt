package kotlin

import java.lang.reflect.Proxy

public fun <T : Annotation> T.annotationType() : Class<out T> {
    val invocationHandler = Proxy.getInvocationHandler(this)!!
    val method = this.javaClass.getMethod("annotationType")
    return invocationHandler.invoke(this, method, array<Object>())!! as Class<out T>
}
