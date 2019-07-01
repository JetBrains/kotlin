/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.parcel

import org.jetbrains.kotlin.android.synthetic.AndroidComponentRegistrar
import org.jetbrains.kotlin.android.synthetic.test.addAndroidExtensionsRuntimeLibrary
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.AbstractAsmLikeInstructionListingTest
import java.io.File

abstract class AbstractParcelBytecodeListingTest : AbstractAsmLikeInstructionListingTest() {
    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        AndroidComponentRegistrar.registerParcelExtensions(environment.project)
        addAndroidExtensionsRuntimeLibrary(environment)
        val androidPluginPath = System.getProperty("ideaSdk.androidPlugin.path")?.takeIf { File(it).isDirectory }
            ?: throw RuntimeException(
                "Unable to get a valid path from 'ideaSdk.androidPlugin.path' property, please point it to the Idea android plugin location"
            )
        environment.updateClasspath(listOf(JvmClasspathRoot(File(androidPluginPath, "layoutlib.jar"))))
    }
}
