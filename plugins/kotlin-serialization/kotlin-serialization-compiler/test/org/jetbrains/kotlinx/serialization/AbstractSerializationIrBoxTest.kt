/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest

open class AbstractSerializationIrBoxTest : AbstractIrBlackBoxCodegenTest() {
    private val coreLibraryPath = getSerializationCoreLibraryJar()
    private val jsonLibraryPath = getSerializationLibraryJar("kotlinx.serialization.json.Json")

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        val librariesPaths = listOf(coreLibraryPath!!, jsonLibraryPath!!)
        builder.configureForKotlinxSerialization(librariesPaths)
    }

}