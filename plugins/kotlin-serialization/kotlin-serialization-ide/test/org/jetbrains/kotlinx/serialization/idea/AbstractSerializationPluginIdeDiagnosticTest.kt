/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationIDEContainerContributor
import java.io.File

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractSerializationPluginIdeDiagnosticTest : AbstractDiagnosticsTest() {
    private val runtimeLibraryPath = getSerializationLibraryRuntimeJar()

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        StorageComponentContainerContributor.registerExtension(environment.project, SerializationIDEContainerContributor())
        environment.updateClasspath(listOf(JvmClasspathRoot(runtimeLibraryPath!!)))
    }

    private fun getSerializationLibraryRuntimeJar(): File? = try {
        PathUtil.getResourcePathForClass(Class.forName("kotlinx.serialization.KSerializer"))
    } catch (e: ClassNotFoundException) {
        null
    }
}
