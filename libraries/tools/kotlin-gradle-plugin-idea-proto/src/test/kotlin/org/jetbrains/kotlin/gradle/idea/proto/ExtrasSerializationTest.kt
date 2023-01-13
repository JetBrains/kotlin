/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto

import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import org.jetbrains.kotlin.tooling.core.toMutableExtras
import kotlin.test.Test
import kotlin.test.fail

class ExtrasSerializationTest : AbstractSerializationTest<MutableExtras>() {
    override fun serialize(value: MutableExtras): ByteArray = value.toByteArray(this)
    override fun deserialize(data: ByteArray): MutableExtras = Extras(data) ?: fail()

    @Test
    fun `sample - extrasWithIntAndStrings`() = testSerialization(TestIdeaKotlinInstances.extrasWithIntAndStrings.toMutableExtras())

    @Test
    fun `sample - emptyExtras`() = testSerialization(mutableExtrasOf())
}