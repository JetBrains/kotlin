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
import java.util.zip.ZipFile

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

    return libDir.listFiles { _, name ->
        // We had a case that this directory contains multiple jar files with "compiler-hosted" name:
        //  - plugins/compose/compiler-hosted/build/libs/compiler-hosted-2.0.20-dev-5638-javadoc.jar
        //  - plugins/compose/compiler-hosted/build/libs/compiler-hosted-2.0.20-dev-5638-sources.jar
        //  - plugins/compose/compiler-hosted/build/libs/compiler-hosted-2.0.20-dev-5642.jar
        //  - plugins/compose/compiler-hosted/build/libs/compiler-hosted-2.0.20-dev-5638.jar
        // The following allow/disallow-list of names is ad-hoc solution, but it helps us to avoid the above jar files.
        // In particular, the above jar files contain compose-compiler-plugin registrar file, so checking registrar does not work.
        name.startsWith("compiler-hosted") && name.endsWith(".jar") && !name.contains("doc") && !name.contains("source")
    }.let { files ->
        when {
            files == null -> error("Can't read the directory $libDir")
            files.isEmpty() -> error("Missing jar file started with \"compiler\" under $COMPOSE_COMPILER_JAR_DIR")
            else -> {
                files.first { it.containsPluginRegistrarFile() }
            }
        }
    }.let { "-Xplugin=${it.absolutePath}" }
}

// Compose compiler plugin uses the old registrar location.
private const val COMPOSE_COMPILER_PLUGIN_REGISTRAR_FILE = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar"

private fun File.containsPluginRegistrarFile(): Boolean = ZipFile(this).use { zipFile ->
    val entry = zipFile.getEntry(COMPOSE_COMPILER_PLUGIN_REGISTRAR_FILE) ?: return false
    val contents = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
    return ComposePluginRegistrar::class.qualifiedName?.let { contents.contains(it) } ?: false
}