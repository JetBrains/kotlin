/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.compiler.plugins

import org.jetbrains.kotlin.allopen.AllOpenEnvironmentConfigurator
import org.jetbrains.kotlin.assignment.plugin.AssignmentPluginEnvironmentConfigurator
import org.jetbrains.kotlin.lombok.LombokAdditionalSourceFileProvider
import org.jetbrains.kotlin.lombok.LombokEnvironmentConfigurator
import org.jetbrains.kotlin.lombok.LombokRuntimeClassPathProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeEnvironmentConfigurator
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeMainClassProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeRuntimeClasspathProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeUtilSourcesProvider
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirLightTreeBlackBoxCodegenTest
import org.jetbrains.kotlin.test.services.jvm.JvmBoxMainClassProvider
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.utils.bind
import org.jetbrains.kotlinx.serialization.SerializationEnvironmentConfigurator
import org.jetbrains.kotlinx.serialization.enableSerializationRuntimeProviders

// ---------------------------- box ----------------------------

open class AbstractPluginInteractionFirBlackBoxCodegenTest : AbstractFirLightTreeBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enablePlugins()
    }
}

// ---------------------------- configuration ----------------------------

/**
 * Note that enabling `ENABLE_PARCELIZE` also requires enabling `REQUIRES_SEPARATE_PROCESS`
 */
fun TestConfigurationBuilder.enablePlugins() {
    useConfigurators(
        ::AllOpenEnvironmentConfigurator,
        ::AssignmentPluginEnvironmentConfigurator,
        ::SerializationEnvironmentConfigurator.bind(/*noLibraries = */false),
        ::LombokEnvironmentConfigurator,
        ::ParcelizeEnvironmentConfigurator
    )

    enableSerializationRuntimeProviders(defaultsProviderBuilder.targetBackend ?: TargetBackend.JVM)
    useCustomRuntimeClasspathProviders(
        ::LombokRuntimeClassPathProvider,
        ::ParcelizeRuntimeClasspathProvider
    )

    useAdditionalSourceProviders(
        ::LombokAdditionalSourceFileProvider,
        ::ParcelizeUtilSourcesProvider
    )

    useAdditionalServices(service<JvmBoxMainClassProvider>(::ParcelizeMainClassProvider))
}
