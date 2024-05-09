/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.services.ComposeExtensionRegistrarConfigurator
import androidx.compose.compiler.plugins.kotlin.services.ComposePluginAnnotationsProvider
import org.jetbrains.kotlin.analysis.api.fir.test.configurators.AnalysisApiFirTestConfiguratorFactory.createConfigurator
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerFacility.AbstractCompilerFacilityTest
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import java.io.File

abstract class AbstractCompilerFacilityTestForComposeCompilerPlugin : AbstractCompilerFacilityTest() {
    override val configurator: AnalysisApiTestConfigurator
        get() = createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                FrontendKind.Fir,
                TestModuleKind.Source,
                AnalysisSessionMode.Normal,
                AnalysisApiMode.Ide
            )
        )

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.composeCompilerPluginConfiguration()
    }
}

fun TestConfigurationBuilder.composeCompilerPluginConfiguration() {
    defaultDirectives {
        flagToEnableComposeCompilerPlugin().let { TestModuleCompiler.Directives.COMPILER_ARGUMENTS + it }
    }

    useConfigurators(
        ::ComposeExtensionRegistrarConfigurator,
        ::ComposePluginAnnotationsProvider,
    )
}

// TODO(KT-68111): Integrate this hard-coded path into compiler-tests-convention.
private const val COMPOSE_COMPILER_JAR_DIR = "plugins/compose/compiler-hosted/build/libs/"

private fun flagToEnableComposeCompilerPlugin(): String {
    val libDir = File(COMPOSE_COMPILER_JAR_DIR)
    if (!libDir.exists() || !libDir.isDirectory) {
        error("No directory \"$COMPOSE_COMPILER_JAR_DIR\" is found")
    }

    return libDir.listFiles { _, name -> name.startsWith("compiler-hosted") && name.endsWith(".jar") && !name.contains("tests") }
        .let { files ->
            when {
                files == null -> error("Can't read the directory $libDir")
                files.isEmpty() -> error("Missing jar file started with \"compiler\" under $COMPOSE_COMPILER_JAR_DIR")
                files.size > 1 -> error("Multiple jar files found ${files.joinToString { it.path }}")
                else -> files.single()
            }
        }.let { "-Xplugin=${it.absolutePath}" }
}
