/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.kpm.AbstractKpmExtensionTest
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinIosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinMacosX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class IdeaKotlinProjectModelSerializableTest : AbstractKpmExtensionTest() {

    @Test
    fun `test - serialize and deserialize - empty project`() {
        project.evaluate()
        assertSerializeAndDeserializeEquals(kotlin.toIdeaKotlinProjectModel())
    }

    @Test
    fun `test - serialize and deserialize - project with variants and fragments`() {
        kotlin.mainAndTest {
            val native = fragments.create("native")
            val apple = fragments.create("apple")
            val ios = fragments.create<KotlinIosX64Variant>("ios")
            val macos = fragments.create<KotlinMacosX64Variant>("macos")
            val linux = fragments.create<KotlinLinuxX64Variant>("linux")
            val jvm = jvm

            apple.refines(native)
            ios.refines(apple)
            macos.refines(apple)
            linux.refines(native)
            jvm.refines(common)
        }

        project.evaluate()
        assertSerializeAndDeserializeEquals(kotlin.toIdeaKotlinProjectModel())
    }

    private fun assertSerializeAndDeserializeEquals(model: IdeaKotlinProjectModel) {
        val byteStream = ByteArrayOutputStream()
        ObjectOutputStream(byteStream).use { stream -> stream.writeObject(model) }
        val serializedModel = byteStream.toByteArray()

        val deserializedModel = ObjectInputStream(ByteArrayInputStream(serializedModel)).use { stream -> stream.readObject() }
        if (deserializedModel !is IdeaKotlinProjectModelImpl) {
            fail("Expected 'deserializedModel' to implement ${IdeaKotlinProjectModelImpl::class.java}. Found $deserializedModel")
        }

        assertEquals(
            model.toString(), deserializedModel.toString(),
            "Expected deserializedModel string representation to match source model"
        )
    }
}
