/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.codegen.AbstractBytecodeListingTest
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationCodegenExtension
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationResolveExtension
import java.io.File

abstract class AbstractBytecodeListingTestForSerialization : AbstractBytecodeListingTest() {
    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        ExpressionCodegenExtension.registerExtension(environment.project, SerializationCodegenExtension())
        SyntheticResolveExtension.registerExtension(environment.project, SerializationResolveExtension())
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        // Hack to add kotlinx-serialization-runtime jar to classpath of test
        configuration.addJvmClasspathRoot(File(Serializable::class.java.protectionDomain.codeSource.location.file))
    }
}
