package kotlin

import java.lang.Class
import java.lang.Object

import jet.runtime.Intrinsic

public val <out T> T.javaClass : Class<T>
    [Intrinsic("kotlin.javaClass.property")] get() = (this as java.lang.Object).getClass() as Class<T>

[Intrinsic("kotlin.javaClass.function")] fun <out T> javaClass() : Class<T> = null as Class<T>
