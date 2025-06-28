/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend

import com.intellij.openapi.project.Project
import org.jebrains.kotlin.backend.native.BasicPhaseContext
import org.jebrains.kotlin.backend.native.PhaseContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

class FrontendServices(val deprecationResolver: DeprecationResolver)

sealed class FrontendPhaseOutput {
    object ShouldNotGenerateCode : FrontendPhaseOutput()

    data class Full(
        val moduleDescriptor: ModuleDescriptor,
        val bindingContext: BindingContext,
        val frontendServices: FrontendServices,
        val environment: KotlinCoreEnvironment,
    ) : FrontendPhaseOutput()
}

interface FrontendContext : PhaseContext {
    var frontendServices: FrontendServices
    val config: NativeFrontendConfig
}

data class FrontendPhaseInput(
    val environment: KotlinCoreEnvironment,
    val project: Project,
)

class FrontendContextImpl(
    override val config: NativeFrontendConfig,
) : BasicPhaseContext(config.configuration), FrontendContext {
    override lateinit var frontendServices: FrontendServices
}