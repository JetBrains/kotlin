package std

import java.lang.Class
import java.lang.Object

val <T> T.javaClass : Class<T>
    get() = jet.runtime.Intrinsics.getJavaClass(this) as Class<T>

val <T> TypeInfo<T>.javaClassForType : Class<T>
    get() = (this as org.jetbrains.jet.rt.TypeInfoImpl<T>).getJavaClass().sure()
