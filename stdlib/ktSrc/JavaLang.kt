package std

import java.lang.Class
import java.lang.Object

import jet.runtime.Intrinsic

val <T> T.javaClass : Class<T>
    [Intrinsic("std.javaClass.property")] get() = (this as java.lang.Object).getClass() as Class<T>

[Intrinsic("std.javaClass.function")] fun <T> javaClass() : Class<T> = null as Class<T>
