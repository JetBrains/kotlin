package kotlin.jvm.internal

import java.lang.annotation.*

Retention(RetentionPolicy.RUNTIME)
annotation class Intrinsic(val value: String)
