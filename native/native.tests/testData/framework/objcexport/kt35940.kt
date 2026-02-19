/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kt35940

import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated(val kClass: KClass<*>)

private interface I1 {
    val s: String
}

private class I1Impl : I1 {
    override val s = "zzz"
}

private class C(var i1: I1?)

private interface I2 {
    fun bar(c: C)
}

private object I2Impl : I2 {
    override fun bar(c: C) {
        c.i1 = I1Impl()
    }
}

@Associated(I2Impl::class)
private class I2ImplHolder

@OptIn(ExperimentalAssociatedObjects::class)
fun testKt35940(): String {
    val i2 = I2ImplHolder::class.findAssociatedObject<Associated>()!! as I2
    val c = C(null)
    i2.bar(c)
    return c.i1!!.s
}
