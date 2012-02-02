package std

import java.lang.Class
import java.lang.Object

val <erased T> T.javaClass : Class<T>
    get() = jet.runtime.Intrinsics.getJavaClass(this) as Class<T>

val <erased T> TypeInfo<T>.javaClassForType : Class<T>
    get() {
      println(this)
      return (this as org.jetbrains.jet.rt.TypeInfoImpl<T>).getJavaClass().sure()
    }
