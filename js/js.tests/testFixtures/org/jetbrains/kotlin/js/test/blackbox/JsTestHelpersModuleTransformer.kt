/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.blackbox

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.ModuleStructureTransformer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider.Companion.detectPackage
import org.jetbrains.kotlin.test.services.sourceProviders.SourceContentView
import org.jetbrains.kotlin.test.testInfraError

/**
 * Moves the additional sources that `BatchingPackageInserter` does not rename into a dedicated module named
 * [HELPERS_MODULE_NAME], which every other module of the test then depends on:
 * - the coroutine helpers of `CoroutineHelpersSourceFilesProvider` (package `helpers`),
 * - the common files of `JsAdditionalSourceProvider` (package `kotlin`).
 *
 * Every test gets the same copy of these sources. Left inside the test modules, they would be declared once per test
 * in a grouped batch and fail the link with `IrSymbol is already bound`. Extracted, a batch links a single helpers KLIB.
 */
@OptIn(TestInfrastructureInternals::class)
object JsTestHelpersModuleTransformer : ModuleStructureTransformer() {
    const val HELPERS_MODULE_NAME: String = "jsTestHelpers"

    private val HELPER_PACKAGES = setOf("helpers", "kotlin")

    override fun transformModuleStructure(
        moduleStructure: TestModuleStructure,
        defaultsProvider: DefaultsProvider
    ): TestModuleStructure {
        val originalModules = moduleStructure.modules
        val helperFilesByPath = linkedMapOf<String, TestFile>()
        for (module in originalModules) {
            for (file in module.files) {
                if (isHelperFile(file)) helperFilesByPath.putIfAbsent(file.relativePath, file)
            }
        }
        if (helperFilesByPath.isEmpty()) return moduleStructure

        val firstModule = originalModules.first()
        val helpersModule = TestModule(
            name = HELPERS_MODULE_NAME,
            files = helperFilesByPath.values.toList(),
            allDependencies = emptyList(),
            directives = firstModule.directives,
            languageVersionSettings = firstModule.languageVersionSettings,
        )
        val helpersDependency = DependencyDescription(
            dependencyModule = helpersModule,
            kind = DependencyKind.Binary,
            relation = DependencyRelation.RegularDependency,
        )

        // Rewrite dependencies recursively so every edge points to the final rewritten module
        // instance, rather than to an intermediate copy with stale dependencies.
        val originalModulesByName = originalModules.associateBy { it.name }
        val rewrittenModulesByName = mutableMapOf(HELPERS_MODULE_NAME to helpersModule)

        fun rewriteModule(module: TestModule): TestModule {
            rewrittenModulesByName[module.name]?.let { return it }

            val rewrittenDependencies = module.allDependencies.map { dependency ->
                val dependencyModuleName = dependency.dependencyModule.name
                val dependencyModule = originalModulesByName[dependencyModuleName]?.let(::rewriteModule)
                    ?: testInfraError("Module $dependencyModuleName not found while rewriting dependencies of ${module.name}")
                dependency.copy(dependencyModule = dependencyModule)
            }

            return module.copy(
                files = module.files.filterNot(::isHelperFile),
                allDependencies = rewrittenDependencies + helpersDependency,
            ).also { rewrittenModulesByName[module.name] = it }
        }

        return TestModuleStructureImpl(
            modules = listOf(helpersModule) + originalModules.map(::rewriteModule),
            originalTestDataFiles = moduleStructure.originalTestDataFiles,
        )
    }

    private fun isHelperFile(file: TestFile): Boolean =
        file.isAdditional && file.name.endsWith(".kt") && detectPackage(file, SourceContentView.ORIGINAL) in HELPER_PACKAGES
}
