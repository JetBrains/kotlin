/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.properties.internal

import java.io.*
import kotlin.properties.internal.UNINITIALIZED
import kotlin.test.Test
import kotlin.test.assertSame

class UninitializedSerializationTest {
    @Test
    fun testSerialization() {
        val out = ByteArrayOutputStream()
        ObjectOutputStream(out).use {
            it.writeObject(UNINITIALIZED)
        }
        println(out.toByteArray().toHexString())
        ObjectInputStream(ByteArrayInputStream(out.toByteArray())).use {
            assertSame(UNINITIALIZED, it.readObject())
        }
    }
}
