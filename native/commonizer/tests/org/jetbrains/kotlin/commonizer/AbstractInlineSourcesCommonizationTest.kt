/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest.DependencyAwareInlineSourceTestFactory
import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest.Parameters
import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.utils.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.test.assertIs
import kotlin.test.fail

data class HierarchicalCommonizationResult(
    val inlineSourceTestFactory: DependencyAwareInlineSourceTestFactory,
    val testParameters: Parameters,
    val commonizerParameters: CommonizerParameters,
    val results: Map<CommonizerTarget, List<ResultsConsumer.ModuleResult>>
)

abstract class AbstractInlineSourcesCommonizationTest : KtInlineSourceCommonizerTestCase() {

    data class Parameters(
        val outputTargets: Set<SharedCommonizerTarget>,
        val dependencies: TargetDependent<List<InlineSourceBuilder.Module>>,
        val supportLibrarySources: Map<CommonizerTarget, InlineSourceBuilder.Module>,
        val targets: List<Target>,
        val settings: CommonizerSettings,
    )

    data class Target(
        val target: CommonizerTarget,
        val modules: List<InlineSourceBuilder.Module>
    )


    @DslMarker
    annotation class InlineSourcesCommonizationTestDsl

    @InlineSourcesCommonizationTestDsl
    class ParametersBuilder(private val parentInlineSourceBuilder: InlineSourceBuilder) {
        private var outputTargets: MutableSet<SharedCommonizerTarget>? = null

        private val dependencies: MutableMap<CommonizerTarget, MutableList<InlineSourceBuilder.Module>> = LinkedHashMap()

        private var targets: List<Target> = emptyList()

        private val supportLibrarySources: MutableMap<CommonizerTarget, InlineSourceBuilder.Module> = LinkedHashMap()

        private val inlineSourceBuilderFactory
            get() = DependencyAwareInlineSourceTestFactory(parentInlineSourceBuilder, dependencies.toTargetDependent())


        fun outputTarget(vararg targets: String) {
            val outputTargets = outputTargets ?: mutableSetOf()
            targets.forEach { target ->
                outputTargets += parseCommonizerTarget(target) as SharedCommonizerTarget
            }
            this.outputTargets = outputTargets
        }

        fun target(target: CommonizerTarget, builder: TargetBuilder.() -> Unit) {
            targets = targets + TargetBuilder(target, inlineSourceBuilderFactory[target]).also(builder).build()
        }

        fun target(target: String, builder: TargetBuilder.() -> Unit) {
            return target(parseCommonizerTarget(target), builder)
        }

        private inline fun registerDependencyFor(
            target: CommonizerTarget,
            dependency: (List<InlineSourceBuilder.Module>) -> InlineSourceBuilder.Module,
        ) {
            val dependenciesList = dependencies.getOrPut(target) { mutableListOf() }
            dependency(dependenciesList).let { dependenciesList.add(it) }
        }

        fun registerDependency(vararg targets: CommonizerTarget, builder: InlineSourceBuilder.ModuleBuilder.() -> Unit) {
            targets.forEach { target ->
                registerDependencyFor(target) { dependenciesList ->
                    inlineSourceBuilderFactory[target].createModule {
                        builder()
                        name = "$target-dependency-${dependenciesList.size}-$name"
                    }
                }
            }
        }

        fun registerDependency(vararg targets: String, builder: InlineSourceBuilder.ModuleBuilder.() -> Unit) {
            registerDependency(targets = targets.map(::parseCommonizerTarget).withAllLeaves().toTypedArray(), builder)
        }

        fun registerSupportLibrary(library: Map<String, InlineSourceBuilder.Module>) {
            val withParsedKeys = library.mapKeys { parseCommonizerTarget(it.key) }.also { supportLibrarySources += it }

            fun CommonizerTarget.getAllContainingSharedModules() = withParsedKeys
                .filter { (target, _) -> allLeaves().isSubsetOf(target.allLeaves()) }

            // To properly compile sample code for output targets (written in `assertEquals()`),
            // we must be able to resolve the resulting types in the dependencies.
            for (it in outputTargets.orEmpty()) {
                val (_, closestSharedSourceSet) = it.getAllContainingSharedModules().minBy { it.key.allLeaves().size }
                registerDependencyFor(it) { closestSharedSourceSet }
            }

            // The commonizer only commonizes `fun foo(Long)` and `fun foo(Int)` if there's at least
            // some typealias in the dependencies that is either `Long` or `Int` specifically, and if
            // it's defined for each target.
            // Unlike the frontend, the commonizer doesn't "see" further `dependsOn` dependencies, so
            // we must add all the common source sets manually.
            for (it in withParsedKeys.keys.allLeaves()) {
                val closestSharedSourceSets = it.getAllContainingSharedModules().values

                for (sourceSet in closestSharedSourceSets) {
                    registerDependencyFor(it) { sourceSet }
                }
            }
        }

        fun simpleSingleSourceTarget(target: CommonizerTarget, @Language("kotlin") sourceContent: String) {
            target(target) {
                module {
                    source(sourceContent)
                }
            }
        }

        infix fun String.withSource(@Language("kotlin") sourceContent: String) {
            simpleSingleSourceTarget(this, sourceContent)
        }

        fun simpleSingleSourceTarget(target: String, @Language("kotlin") sourceCode: String) {
            simpleSingleSourceTarget(parseCommonizerTarget(target), sourceCode)
        }

        fun <T : Any> setting(type: CommonizerSettings.Key<T>, value: T) {
            val setting = MapBasedCommonizerSettings.Setting(type, value)
            check(setting.key !in settings.map { it.key }) {
                "An attempt to add the same setting '${type::class.java.simpleName}' multiple times. " +
                        "Current value: '$value'; Previous value: '${settings.find { it.key == setting.key }!!.settingValue}'"
            }

            settings.add(setting)
        }

        private val settings: MutableSet<MapBasedCommonizerSettings.Setting<*>> = LinkedHashSet()

        fun build(): Parameters = Parameters(
            outputTargets = outputTargets ?: setOf(SharedCommonizerTarget(targets.map { it.target }.allLeaves())),
            dependencies = dependencies.toTargetDependent(),
            supportLibrarySources = supportLibrarySources,
            targets = targets.toList(),
            settings = MapBasedCommonizerSettings(*settings.toTypedArray()),
        )
    }

    data class DependencyAwareInlineSourceTestFactory(
        private val inlineSourceBuilder: InlineSourceBuilder,
        private val dependencies: TargetDependent<List<InlineSourceBuilder.Module>>
    ) {
        operator fun get(target: CommonizerTarget): InlineSourceBuilder {
            return object : InlineSourceBuilder by inlineSourceBuilder {
                override fun createModule(builder: InlineSourceBuilder.ModuleBuilder.() -> Unit): InlineSourceBuilder.Module {
                    return inlineSourceBuilder.createModule {
                        dependencies.toMap()
                            .filterKeys { dependencyTarget -> target in dependencyTarget.withAllLeaves() }.values.flatten()
                            .forEach { dependencyModule -> dependency(dependencyModule) }
                        builder()
                    }
                }
            }
        }
    }

    @InlineSourcesCommonizationTestDsl
    class TargetBuilder(private val target: CommonizerTarget, private val inlineSourceBuilder: InlineSourceBuilder) {
        private var modules: List<InlineSourceBuilder.Module> = emptyList()

        fun module(builder: InlineSourceBuilder.ModuleBuilder.() -> Unit) {
            modules = modules + inlineSourceBuilder.createModule(builder)
        }

        fun build(): Target = Target(target, modules = modules)
    }

    fun commonize(
        expectedStatus: ResultsConsumer.Status = ResultsConsumer.Status.DONE,
        builder: ParametersBuilder.() -> Unit
    ): HierarchicalCommonizationResult {
        val consumer = MockResultsConsumer()
        val testParameters = ParametersBuilder(this).also(builder).build()
        val commonizerParameters = testParameters.toCommonizerParameters(consumer)
        runCommonization(commonizerParameters)
        assertEquals(expectedStatus, consumer.status)
        return HierarchicalCommonizationResult(
            inlineSourceTestFactory = DependencyAwareInlineSourceTestFactory(inlineSourceBuilder, testParameters.dependencies),
            testParameters = testParameters,
            commonizerParameters = commonizerParameters,
            results = consumer.modulesByTargets.mapValues { [_, collection] -> collection.toList() }
        )
    }


    private fun Parameters.toCommonizerParameters(
        resultsConsumer: ResultsConsumer,
        manifestDataProvider: (CommonizerTarget) -> NativeManifestDataProvider = { MockNativeManifestDataProvider(it) },
    ): CommonizerParameters {
        return CommonizerParameters(
            outputTargets = outputTargets,
            manifestProvider = TargetDependent(outputTargets, manifestDataProvider),
            dependenciesProvider = TargetDependent(outputTargets.withAllLeaves()) { target ->
                val dependenciesMetadata = dependencies.getOrNull(target).orEmpty()
                    .map { module -> createMetadata(module) }
                    .plus(loadStdlibMetadata())
                MockModulesProvider.create(dependenciesMetadata)
            },
            supportLibraryModulesProvider = TargetDependent(outputTargets.withAllLeaves()) { target ->
                val modules = supportLibrarySources
                    .filterKeys { supportTarget -> target.allLeaves().isSubsetOf(supportTarget.allLeaves()) }
                    .values.map { createMetadata(it) }
                MockModulesProvider.create(modules)
            },
            targetProviders = TargetDependent(outputTargets.allLeaves()) { commonizerTarget ->
                val target = targets.singleOrNull { it.target == commonizerTarget } ?: return@TargetDependent null
                TargetProvider(
                    target = commonizerTarget,
                    modulesProvider = MockModulesProvider.create(
                        target.modules.map { createMetadata(it) }
                    )
                )
            },
            resultsConsumer = resultsConsumer,
            settings = settings,
        )
    }
}


/* ASSERTIONS */

fun HierarchicalCommonizationResult.getTarget(target: CommonizerTarget): List<ResultsConsumer.ModuleResult> {
    return this.results[target] ?: fail("Missing target $target in results ${this.results.keys}")
}

fun HierarchicalCommonizationResult.assertCommonized(
    target: CommonizerTarget,
    moduleBuilder: InlineSourceBuilder.ModuleBuilder.() -> Unit
) {
    val inlineSourceTest = inlineSourceTestFactory[target]

    val referenceModule = inlineSourceTest.createModule {
        moduleBuilder()
    }

    val module = getTarget(target).firstOrNull { moduleResult -> moduleResult.libraryName == referenceModule.name }
        ?: fail("Missing ${referenceModule.name} in target $target")

    val commonizedModule = assertIs<ResultsConsumer.ModuleResult>(module, "Expected ${module.libraryName} to be 'Commonized'")

    assertModulesAreEqual(
        inlineSourceTest.createMetadata(referenceModule).metadata, commonizedModule.metadata, target
    )
}

fun HierarchicalCommonizationResult.assertCommonized(target: CommonizerTarget, @Language("kotlin") sourceContent: String) {
    assertCommonized(target) {
        source(sourceContent)
    }
}

fun HierarchicalCommonizationResult.assertCommonized(target: String, @Language("kotlin") sourceContent: String) =
    assertCommonized(parseCommonizerTarget(target), sourceContent)

fun HierarchicalCommonizationResult.assertCommonized(
    target: String,
    moduleBuilder: InlineSourceBuilder.ModuleBuilder.() -> Unit
) = assertCommonized(parseCommonizerTarget(target), moduleBuilder)
