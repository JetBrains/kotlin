/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTest : AbstractSerializationTest<IdeaKpmPlatform>() {

    override fun serialize(value: IdeaKpmPlatform): ByteArray {
        return IdeaKpmPlatformProto(value).toByteArray()
    }

    override fun deserialize(data: ByteArray): IdeaKpmPlatform {
        return IdeaKpmPlatform(IdeaKpmPlatformProto.parseFrom(data))
    }

    @Test
    fun `serialize - deserialize - jvm`() {
        val value = IdeaKpmJvmPlatformImpl("jvmTarget")
        assertEquals(value, IdeaKpmJvmPlatform(value.toByteArray(this)))
        assertEquals(value, IdeaKpmPlatform(IdeaKpmPlatformProto(value)))
        testSerialization(value)
    }

    @Test
    fun `serialize - deserialize - native`() {
        val value = IdeaKpmNativePlatformImpl("konanTarget")
        assertEquals(value, IdeaKpmNativePlatform(value.toByteArray(this)))
        assertEquals(value, IdeaKpmPlatform(IdeaKpmPlatformProto(value)))
        testSerialization(value)
    }

    @Test
    fun `serialize - deserialize - js`() {
        val value = IdeaKpmJsPlatformImpl(true)
        assertEquals(value, IdeaKpmJsPlatform(value.toByteArray(this)))
        assertEquals(value, IdeaKpmPlatform(IdeaKpmPlatformProto(value)))
        testSerialization(value)
    }

    @Test
    fun `serialize - deserialize - wasm`() {
        val value = IdeaKpmWasmPlatformImpl()
        assertEquals(value, IdeaKpmWasmPlatform(value.toByteArray(this)))
        assertEquals(value, IdeaKpmPlatform(IdeaKpmPlatformProto(value)))
        testSerialization(value)
    }

    @Test
    fun `serialize - deserialize - unknown`() {
        val value = IdeaKpmUnknownPlatformImpl()
        assertEquals(value, IdeaKpmUnknownPlatform(value.toByteArray(this)))
        assertEquals(value, IdeaKpmPlatform(IdeaKpmPlatformProto(value)))
        testSerialization(value)
    }
}
