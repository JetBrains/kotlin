/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil

abstract class AbstractSerializationQuickFixTest : AbstractQuickFixTest() {
    override fun setUp() {
        super.setUp()
        val coreJar = getSerializationCoreLibraryJar()!!
        val jsonJar = getSerializationJsonLibraryJar()!!
        ConfigLibraryUtil.addLibrary(module, "Serialization core", coreJar.parentFile.absolutePath, arrayOf(coreJar.name))
        ConfigLibraryUtil.addLibrary(module, "Serialization JSON", jsonJar.parentFile.absolutePath, arrayOf(jsonJar.name))
    }

    override fun tearDown() {
        ConfigLibraryUtil.removeLibrary(module, "Serialization JSON")
        ConfigLibraryUtil.removeLibrary(module, "Serialization core")

        super.tearDown()
    }
}
