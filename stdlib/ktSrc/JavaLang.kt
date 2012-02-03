package std

import java.lang.Class
import java.lang.Object

val <erased T> T.javaClass : Class<T>
    get() = jet.runtime.Intrinsics.getJavaClass(this) as Class<T>

fun <T> javaClass() : Class<T> = (typeinfo<T>() as org.jetbrains.jet.rt.TypeInfoImpl<T>).getJavaClass().sure()

val <erased T> TypeInfo<T>.javaClassForType : Class<T>
    get() {
      return (this as org.jetbrains.jet.rt.TypeInfoImpl<T>).getJavaClass().sure()
    }
