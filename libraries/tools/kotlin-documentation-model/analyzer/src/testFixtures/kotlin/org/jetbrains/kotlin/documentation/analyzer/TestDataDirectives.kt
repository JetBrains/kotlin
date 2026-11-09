/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.documentation.KdBundle
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.directives.model.ComposedDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.model.ArtifactKind
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.DefaultRegisteredDirectivesProvider
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.impl.ModuleStructureExtractorImpl
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import java.io.File

/**
 * A single parsed `// MODULE:` block of a test data file, corresponding 1:1 to a [KdAnalyzerSourceSet].
 */
internal data class TestDataModule(
    val name: String,
    val platform: KdAnalyzerPlatform,
    val dependsOn: Set<String>,
    val files: Map<String, String>,
)

private object KdmTestDirectives : SimpleDirectivesContainer() {
    val PLATFORM: ValueDirective<KdAnalyzerPlatform> by enumDirective(
        description = "Platform of the documentation analyzer source set (JVM by default).",
        applicability = DirectiveApplicability.Module,
    )
}

/** Adapts the shared compiler fixture format to documentation analyzer source sets. */
@OptIn(TestInfrastructureInternals::class)
internal fun parseTestDataModules(testDataFile: File): List<TestDataModule> {
    // parseModuleStructureWithoutService() omits KotlinTestInfo, but dependency lookup may request it
    // while computing escaped names even when escaping is disabled (e.g. intermediateSourceSet.kt).
    // Create the services locally so we can supply that metadata without changing the shared extractor.
    val services = TestServices().apply {
        register(AssertionsService::class, JUnit5Assertions)
        // Dependency lookup may compute escaped names even though KDM keeps module names unchanged.
        register(KotlinTestInfo::class, KotlinTestInfo("KdmAnalyzer", testDataFile.nameWithoutExtension, emptySet()))
        register(
            DefaultsProvider::class,
            DefaultsProvider(
                frontendKind = FrontendKind.NoFrontend,
                backendKind = BackendKind.NoBackend,
                defaultLanguageSettingsBuilder = LanguageVersionSettingsBuilder(),
                targetPlatform = CommonPlatforms.defaultCommonPlatform,
                artifactKind = ArtifactKind.NoArtifact,
                targetBackend = null,
                defaultDependencyKind = DependencyKind.Source,
            ),
        )
        register(DefaultRegisteredDirectivesProvider::class, DefaultRegisteredDirectivesProvider(RegisteredDirectives.Empty))
    }
    val structure = ModuleStructureExtractorImpl(
        services,
        additionalSourceProviders = emptyList(),
        moduleStructureTransformers = emptyList(),
        environmentConfigurators = emptyList(),
    ).splitTestDataByModules(
        testDataFile.canonicalPath,
        ComposedDirectivesContainer(ModuleStructureDirectives, LanguageSettingsDirectives, KdmTestDirectives),
    )
    return structure.modules.map { module ->
        TestDataModule(
            name = module.name,
            platform = module.directives.singleOrZeroValue(KdmTestDirectives.PLATFORM) ?: KdAnalyzerPlatform.JVM,
            dependsOn = module.dependsOnDependencies.mapTo(linkedSetOf()) { it.dependencyModule.name },
            files = module.files.associate { file ->
                // The shared extractor retains fixture headers and pads files to their original line numbers.
                // KDM round-trip tests need only the source following those headers.
                val lines = file.originalContent.lines()
                val headerEnd = lines.indexOfLast {
                    RegisteredDirectivesParser.parseDirective(it)?.name in setOf("MODULE", "PLATFORM", "FILE")
                }
                file.relativePath to lines.drop(headerEnd + 1).joinToString("\n")
            },
        )
    }
}

private val testDataDir = File("src/test/resources/testData/analyzer")

/** The test data file `<name>` under `src/test/resources/testData/analyzer`. */
internal fun testDataFile(name: String): File = File(testDataDir, name.substringAfter("/testData/analyzer/"))

/** Materializes the modules parsed from [file] on disk and runs [generateDocumentationBundle] over them. */
internal fun generateDocumentationBundleFromTestData(file: File): KdBundle {
    val sourceSets = parseTestDataModules(file).map { module ->
        val root = createTempSourceRoot()
        for ([path, content] in module.files) {
            File(root, path).apply { parentFile.mkdirs() }.writeText(content)
        }
        KdAnalyzerSourceSet(
            name = module.name,
            platform = module.platform,
            sourceRoots = setOf(root),
            classpath = listOf(if (module.platform == KdAnalyzerPlatform.NATIVE) nativeStdlibKlib else jvmStdlibJar),
            dependsOn = module.dependsOn,
        )
    }
    return generateDocumentationBundle(moduleName = file.nameWithoutExtension, sourceSets = sourceSets)
}
