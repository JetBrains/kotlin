/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated1(val kClass: KClass<*>)

@OptIn(ExperimentalAssociatedObjects::class)
fun box(): String {
    val i2 = I2ImplHolder()::class.findAssociatedObject<Associated1>()!! as I2
    assertEquals(17, i2.foo())

    return "OK"
}

private interface I2 {
    fun foo(): Int
}

private object I2Impl : I2 {
    override fun foo() = 17
}

@Associated1(I2Impl::class)
private class I2ImplHolder