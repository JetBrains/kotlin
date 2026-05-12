/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives.WITH_COROUTINES
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.ModuleStructureTransformer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

/**
 * Extracts files belonging to the synthetic `helpers` package (added by
 * `CoroutineHelpersSourceFilesProvider` when a test uses `// WITH_COROUTINES`) from each test
 * module and places them into a single dedicated module named [HELPERS_MODULE_NAME].
 *
 * The original test modules then get a regular `DependencyKind.Binary` dependency on the new
 * helpers module instead of carrying the helper sources themselves.
 *
 * This allows test grouping in the Wasm grouping stage to batch multiple tests together
 * without producing duplicate `helpers/EmptyContinuation` etc. declarations across per-test
 * KLIBs, which previously caused
 * `IllegalStateException: IrClassSymbolImpl is already bound. Signature: helpers/...` at link
 * time. The grouping stage simply links a single shared `helpers.klib` for the whole batch.
 */
@OptIn(TestInfrastructureInternals::class)
object WasmCoroutineHelpersModuleTransformer : ModuleStructureTransformer() {
    const val HELPERS_MODULE_NAME: String = "helpers"

    override fun transformModuleStructure(
        moduleStructure: TestModuleStructure,
        defaultsProvider: DefaultsProvider
    ): TestModuleStructure {
        val originalModules = moduleStructure.modules
        if (originalModules.isEmpty()) return moduleStructure

        // We only act when at least one module has actual helpers files attached.
        val hasAnyHelperFile = originalModules.any { module ->
            module.files.any { isHelpersFile(it) }
        }
        if (!hasAnyHelperFile) return moduleStructure

        // Don't transform if WITH_COROUTINES isn't part of the test (extra safety; helper files
        // shouldn't otherwise be present).
        val hasWithCoroutines = originalModules.any { WITH_COROUTINES in it.directives } ||
                originalModules.any { module -> module.files.any { WITH_COROUTINES in it.directives } }
        if (!hasWithCoroutines) return moduleStructure

        // 1. Collect all helper files from all modules (deduplicated by relative path).
        val helperFilesByPath = linkedMapOf<String, TestFile>()
        for (module in originalModules) {
            for (file in module.files) {
                if (isHelpersFile(file)) {
                    helperFilesByPath.putIfAbsent(file.relativePath, file)
                }
            }
        }

        // 2. Build the new helpers module.
        //    - Uses the same language version settings as the first original module.
        //    - Has no dependencies of its own.
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

        // 3. Strip helper files from each original module and add the dependency.
        val rewrittenModules = originalModules.map { module ->
            val filtered = module.files.filterNot { isHelpersFile(it) }
            // Avoid duplicating the dependency if it's already present (defensive).
            val depsAlreadyContainsHelpers = module.allDependencies.any { it.dependencyModule.name == HELPERS_MODULE_NAME }
            val newDeps = if (depsAlreadyContainsHelpers) module.allDependencies
            else module.allDependencies + helpersDependency
            module.copy(files = filtered, allDependencies = newDeps)
        }

        return TestModuleStructureImpl(
            modules = listOf(helpersModule) + rewrittenModules,
            originalTestDataFiles = moduleStructure.originalTestDataFiles,
        )
    }

    /**
     * Returns true if the given file is one of the synthetic coroutine-helper files produced
     * by `CoroutineHelpersSourceFilesProvider`. Such files are marked as additional and have
     * `package helpers` as their package declaration.
     */
    private fun isHelpersFile(file: TestFile): Boolean {
        if (!file.isAdditional) return false
        // Cheap content check: helper files start with `package helpers`.
        val content = file.originalContent
        // Either it begins with `package helpers` (first line), or contains it as a
        // non-commented line at the top.
        val firstNonEmptyLine = content.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
        return firstNonEmptyLine.trim().startsWith("package helpers")
    }
}
