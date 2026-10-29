/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.compiler.plugins

import org.jetbrains.kotlin.allopen.AllOpenEnvironmentConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLReversedBlackBoxTest
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.assignment.plugin.AssignmentPluginEnvironmentConfigurator
import org.jetbrains.kotlin.js.test.runners.AbstractJsTest
import org.jetbrains.kotlin.lombok.LombokEnvironmentConfigurator
import org.jetbrains.kotlin.lombok.LombokRuntimeClassPathProvider
import org.jetbrains.kotlin.noarg.NoArgEnvironmentConfigurator
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeEnvironmentConfigurator
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeMainClassProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeRuntimeClasspathProvider
import org.jetbrains.kotlin.parcelize.test.services.ParcelizeUtilSourcesProvider
import org.jetbrains.kotlin.plugin.sandbox.ExtensionRegistrarConfigurator
import org.jetbrains.kotlin.plugin.sandbox.PluginAnnotationsProvider
import org.jetbrains.kotlin.plugin.sandbox.PluginRuntimeAnnotationsProvider
import org.jetbrains.kotlin.plugin.sandbox.PluginSandboxDirectives.ENABLE_COMPANION_SUPERTYPE_EXTENSION
import org.jetbrains.kotlin.powerassert.PowerAssertEnvironmentConfigurator
import org.jetbrains.kotlin.test.FirParser
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
        builder.enablePlugins(targetBackend)
    }
}

/** Runs the same `box` test data as [AbstractPluginInteractionFirBlackBoxCodegenTest] on the JS backend. */
open class AbstractPluginInteractionJsBlackBoxCodegenTest : AbstractJsTest(
    pathToTestDir = "plugins/plugins-interactions-testing/testData/box",
    testGroupOutputDirPrefix = "pluginInteractionsBox/",
    parser = FirParser.LightTree,
) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enablePlugins(targetBackend)
    }
}

// ---------------------------- LL FIR (IDE) diagnostics ----------------------------

abstract class AbstractPluginInteractionLLDiagnosticsTest : AbstractLLDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enablePlugins()
    }
}

// ---------------------------- LL FIR (IDE) black box ----------------------------

/** Runs the same `box` test data as [AbstractPluginInteractionFirBlackBoxCodegenTest] through the LL FIR frontend only. */
abstract class AbstractPluginInteractionLLBlackBoxTest : AbstractLLBlackBoxTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enablePlugins(tolerateMissingExpectedException = true)
        builder.ignoreLombokConfigFile()
    }
}

/** A reversed counterpart of [AbstractPluginInteractionLLBlackBoxTest]. */
abstract class AbstractPluginInteractionLLReversedBlackBoxTest : AbstractLLReversedBlackBoxTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enablePlugins(tolerateMissingExpectedException = true)
        builder.ignoreLombokConfigFile()
    }
}

// ---------------------------- configuration ----------------------------

/**
 * Note that enabling `ENABLE_PARCELIZE` also requires enabling `REQUIRES_SEPARATE_PROCESS`
 *
 * [targetBackend] is a parameter, because the test runner sets the target backend of the configuration only after `configure`.
 *
 * Set [tolerateMissingExpectedException] to true for an analysis-only run, which never reaches the backend.
 */
fun TestConfigurationBuilder.enablePlugins(
    targetBackend: TargetBackend = TargetBackend.JVM,
    tolerateMissingExpectedException: Boolean = false,
) {
    useConfigurators(
        ::AllOpenEnvironmentConfigurator,
        ::AssignmentPluginEnvironmentConfigurator,
        ::SerializationEnvironmentConfigurator.bind(/*noLibraries = */false),
        ::LombokEnvironmentConfigurator,
        ::NoArgEnvironmentConfigurator,
        ::ParcelizeEnvironmentConfigurator,
        ::PowerAssertEnvironmentConfigurator,
        // Plugin sandbox configurators.
        ::ExtensionRegistrarConfigurator, ::PluginAnnotationsProvider,
    )

    enableSerializationRuntimeProviders(targetBackend)
    useCustomRuntimeClasspathProviders(
        ::LombokRuntimeClassPathProvider,
        ::ParcelizeRuntimeClasspathProvider,
        ::PluginRuntimeAnnotationsProvider,
    )

    useAdditionalSourceProviders(
        ::ParcelizeUtilSourcesProvider,
    )

    useAdditionalServices(service<JvmBoxMainClassProvider>(::ParcelizeMainClassProvider))

    useFailureSuppressors(::ExpectedExceptionSuppressor.bind(tolerateMissingExpectedException))

    forTestsMatching("*/generatedSupertypes/*") {
        defaultDirectives { +ENABLE_COMPANION_SUPERTYPE_EXTENSION }
    }
}

/**
 * The Lombok configurator writes a `lombok.config` into the module.
 * The Analysis API builds PSI for every module file, so it must be told that this file is a resource and not an unrecognized source.
 */
private fun TestConfigurationBuilder.ignoreLombokConfigFile() {
    defaultDirectives { +AnalysisApiTestDirectives.IGNORE_UNKNOWN_FILES }
}
