/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok

import com.intellij.openapi.project.Project
import lombok.Getter
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.codegen.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.test.runners.configurationForClassicAndFirTestsAlongside
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

// ---------------------------- box ----------------------------

open class AbstractBlackBoxCodegenTestForLombok : AbstractBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

open class AbstractIrBlackBoxCodegenTestForLombok : AbstractIrBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

open class AbstractFirBlackBoxCodegenTestForLombok : AbstractFirBlackBoxCodegenTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

// ---------------------------- diagnostics ----------------------------

open class AbstractDiagnosticTestForLombok : AbstractDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.enableLombok()
    }
}

open class AbstractFirDiagnosticTestForLombok : AbstractFirDiagnosticTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configurationForClassicAndFirTestsAlongside()
        builder.enableLombok()
    }
}

// ---------------------------- configuration ----------------------------

fun TestConfigurationBuilder.enableLombok() {
    useConfigurators(::LombokEnvironmentConfigurator)
    useAdditionalSourceProviders(::LombokAdditionalSourceFileProvider)
}

class LombokAdditionalSourceFileProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        const val COMMON_SOURCE_PATH = "plugins/lombok/testData/common.kt"
    }

    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        return listOf(File(COMMON_SOURCE_PATH).toTestFile())
    }
}

class LombokEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val LOMBOK_CONFIG_NAME = "lombok.config"
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoot(PathUtil.getResourcePathForClass(Getter::class.java))

        val lombokConfig = findLombokConfig(module) ?: return
        lombokConfig.copyTo(testServices.sourceFileProvider.javaSourceDirectory.resolve(lombokConfig.name))
        configuration.put(LombokConfigurationKeys.CONFIG_FILE, lombokConfig)
    }

    private fun findLombokConfig(module: TestModule): File? {
        return module.files.singleOrNull { it.name == LOMBOK_CONFIG_NAME }?.let {
            testServices.sourceFileProvider.getRealFileForSourceFile(it)
        }
    }

    override fun registerCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        LombokComponentRegistrar.registerComponents(project, configuration)
    }
}
