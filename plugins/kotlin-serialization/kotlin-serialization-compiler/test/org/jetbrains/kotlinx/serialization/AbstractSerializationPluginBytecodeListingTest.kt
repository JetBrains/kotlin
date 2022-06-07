/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.AbstractAsmLikeInstructionListingTest
import org.jetbrains.kotlin.compiler.plugin.registerExtensionsForTest
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractSerializationPluginBytecodeListingTest : AbstractAsmLikeInstructionListingTest() {
    private val coreLibraryPath = getSerializationCoreLibraryJar()

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        registerExtensionsForTest(environment.project, environment.configuration) {
            SerializationComponentRegistrar.registerExtensions(this)
        }
        environment.updateClasspath(listOf(JvmClasspathRoot(coreLibraryPath!!)))
    }
}
