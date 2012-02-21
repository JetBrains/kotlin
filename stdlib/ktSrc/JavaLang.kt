package std

import java.lang.Class
import java.lang.Object

import jet.runtime.intrinsic

val <T> T.javaClass : Class<T>
    [intrinsic("std.javaClass.property")] get() = (this as java.lang.Object).getClass() as Class<T>

[intrinsic("std.javaClass.function")] fun <T> javaClass() : Class<T> = null as Class<T>
