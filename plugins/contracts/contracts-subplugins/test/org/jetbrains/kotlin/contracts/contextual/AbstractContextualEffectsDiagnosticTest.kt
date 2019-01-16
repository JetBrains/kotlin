/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual

import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.extensions.ContractsExtension
import org.jetbrains.kotlin.serialization.ContractSerializerExtension
import java.io.File

abstract class AbstractContextualEffectsDiagnosticTest : AbstractDiagnosticsTest() {
    override fun createEnvironment(file: File): KotlinCoreEnvironment {
        val environment = super.createEnvironment(file)
        ContractsExtension.registerExtensionPoint(environment.project)
        ContractSerializerExtension.registerExtensionPoint(environment.project)
        registerExtensions(environment.project)
        return environment
    }

    override fun getExtraClasspath(): List<File> = contractsDslClasspath
}