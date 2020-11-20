/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class ParcelizeComponentRegistrar : ComponentRegistrar {
    companion object {
        fun registerParcelizeComponents(project: Project) {
            ExpressionCodegenExtension.registerExtension(project, ParcelizeCodegenExtension())
            IrGenerationExtension.registerExtension(project, ParcelizeIrGeneratorExtension())
            SyntheticResolveExtension.registerExtension(project, ParcelizeResolveExtension())
            ClassBuilderInterceptorExtension.registerExtension(project, ParcelizeClinitClassBuilderInterceptorExtension())
            StorageComponentContainerContributor.registerExtension(project, ParcelizeDeclarationCheckerComponentContainerContributor())
        }
    }

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerParcelizeComponents(project)
    }
}

class ParcelizeDeclarationCheckerComponentContainerContributor : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor,
    ) {
        if (platform.isJvm()) {
            container.useInstance(ParcelizeDeclarationChecker())
            container.useInstance(ParcelizeAnnotationChecker())
        }
    }
}