/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.test.blackbox.support.util.generateBoxFunctionLauncher
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager

private const val LAUNCHER_FILE_NAME = "__launcher__.kt"
private val BOX_FUNCTION_NAME = Name.identifier("box")

class NativeLauncherAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        if (!NativeEnvironmentConfigurator.isMainModule(module, testModuleStructure))
            return emptyList()

        val boxFunctionFqn = findBoxFunctionFqn(module)
            ?: BOX_FUNCTION_NAME.asString() // Should metatestconfigurators allow the test to run, let the frontend raise error "No box() function found")

        val launcherContent = generateBoxFunctionLauncher(boxFunctionFqn)
        val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("launcher")
        val launcherFile = tempDir.resolve(LAUNCHER_FILE_NAME).also {
            it.writeText(launcherContent)
        }

        return listOf(
            TestFile(
                relativePath = LAUNCHER_FILE_NAME,
                originalContent = launcherContent,
                originalFile = launcherFile,
                startLineNumberInOriginalFile = 0,
                isAdditional = true,
                directives = RegisteredDirectives.Empty
            )
        )
    }

    private fun findBoxFunctionFqn(module: TestModule): String? {
        val disposable = Disposer.newDisposable("Disposable for NativeLauncherAdditionalSourceProvider")
        try {
            val psiFactory = createPsiFactory(disposable)
            for (file in module.files) {
                if (!file.name.endsWith(".kt")) continue
                val ktFile = psiFactory.createFile(file.name, file.originalContent)
                if (ktFile.getChildrenOfType<KtNamedFunction>().any { function ->
                        function.name == BOX_FUNCTION_NAME.asString() && function.valueParameters.isEmpty()
                    }) return ktFile.packageFqName.child(BOX_FUNCTION_NAME).asString()
            }
            return null
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun createPsiFactory(parentDisposable: Disposable): KtPsiFactory {
        val configuration = CompilerConfiguration()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "native-launcher-source-provider-module")

        @OptIn(K1Deprecation::class)
        val environment = KotlinCoreEnvironment.createForProduction(
            projectDisposable = parentDisposable,
            configuration = configuration,
            configFiles = EnvironmentConfigFiles.METADATA_CONFIG_FILES
        )

        return KtPsiFactory(environment.project)
    }
}
