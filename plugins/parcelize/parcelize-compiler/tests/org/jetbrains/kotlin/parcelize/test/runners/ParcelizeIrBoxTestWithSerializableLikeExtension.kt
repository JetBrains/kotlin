/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.test.runners

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.parcelize.test.services.SerializableLikeExtensionProvider
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.junit.jupiter.api.Test

@TestMetadata("plugins/parcelize/parcelize-compiler/testData/box")
@TestDataPath("\$PROJECT_ROOT")
class ParcelizeIrBoxTestWithSerializableLikeExtension : AbstractParcelizeIrBoxTest() {
    @Test
    @TestMetadata("simple.kt")
    fun testSimple() {
        runTest("plugins/parcelize/parcelize-compiler/testData/box/simple.kt")
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useConfigurators(::SerializableLikeExtensionProvider)
    }
}


@TestMetadata("plugins/parcelize/parcelize-compiler/testData/box")
@TestDataPath("\$PROJECT_ROOT")
class ParcelizeFirBoxTestWithSerializableLikeExtension : AbstractParcelizeFirLightTreeBoxTest() {
    @Test
    @TestMetadata("simple.kt")
    fun testSimple() {
        runTest("plugins/parcelize/parcelize-compiler/testData/box/simple.kt")
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.useConfigurators(::SerializableLikeExtensionProvider)
    }
}

