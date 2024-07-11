/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// FILE: a.kt
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class Associated(val kClass: KClass<*>)

object Bar

// FILE: b.kt
@Associated(Bar::class)
class Foo

// FILE: main.kt
import kotlin.test.*
import kotlin.reflect.*

@OptIn(ExperimentalAssociatedObjects::class)
fun box(): String {
    assertSame(Bar, Foo::class.findAssociatedObject<Associated>())
    assertSame(null, Bar::class.findAssociatedObject<Associated>())

    return "OK"
}
