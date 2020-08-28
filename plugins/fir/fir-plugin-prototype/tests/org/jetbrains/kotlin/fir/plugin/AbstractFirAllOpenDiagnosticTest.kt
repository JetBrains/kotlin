/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.AbstractFirDiagnosticsTest
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import java.io.File

abstract class AbstractFirAllOpenDiagnosticTest : AbstractFirDiagnosticsTest() {
    override val pluginPhasesEnabled: Boolean
        get() = true

    override fun getFirExtensions(): BunchOfRegisteredExtensions {
        return FirAllOpenComponentRegistrar().configure()
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        val jar = File("plugins/fir/fir-plugin-prototype/plugin-annotations/build/libs/plugin-annotations-1.4.255-SNAPSHOT.jar")
        if (!jar.exists()) {
            throw AssertionError("Jar with annotations does not exist. Please run :plugins:fir:fir-plugin-prototype:plugin-annotations:jar")
        }
        configuration.addJvmClasspathRoot(jar)
    }
}
