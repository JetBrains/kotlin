/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest.DependencyAwareInlineSourceTestFactory
import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest.Parameters
import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult.Commonized
import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.utils.*
import kotlin.test.assertIs

data class HierarchicalCommonizationResult(
    val inlineSourceTestFactory: DependencyAwareInlineSourceTestFactory,
    val testParameters: Parameters,
    val commonizerParameters: CommonizerParameters,
    val results: Map<CommonizerTarget, List<ResultsConsumer.ModuleResult>>
)

abstract class AbstractInlineSourcesCommonizationTest : KtInlineSourceCommonizerTestCase() {

    data class Parameters(
        val outputTarget: SharedCommonizerTarget,
        val dependencies: TargetDependent<List<InlineSourceBuilder.Module>>,
        val targets: List<Target>
    )

    data class Target(
        val target: CommonizerTarget,
        val modules: List<InlineSourceBuilder.Module>
    )


    @DslMarker
    annotation class InlineSourcesCommonizationTestDsl

    @InlineSourcesCommonizationTestDsl
    class ParametersBuilder(private val parentInlineSourceBuilder: InlineSourceBuilder) {
        private var outputTarget: SharedCommonizerTarget? = null

        private val dependencies: MutableMap<CommonizerTarget, MutableList<InlineSourceBuilder.Module>> = LinkedHashMap()

        private var targets: List<Target> = emptyList()

        private val inlineSourceBuilderFactory
            get() = DependencyAwareInlineSourceTestFactory(parentInlineSourceBuilder, dependencies.toTargetDependent())


        @InlineSourcesCommonizationTestDsl
        fun outputTarget(target: String) {
            outputTarget = parseCommonizerTarget(target) as SharedCommonizerTarget
        }

        @InlineSourcesCommonizationTestDsl
        fun target(target: CommonizerTarget, builder: TargetBuilder.() -> Unit) {
            targets = targets + TargetBuilder(target, inlineSourceBuilderFactory[target]).also(builder).build()
        }

        @InlineSourcesCommonizationTestDsl
        fun target(target: String, builder: TargetBuilder.() -> Unit) {
            return target(parseCommonizerTarget(target), builder)
        }

        @InlineSourcesCommonizationTestDsl
        fun registerDependency(target: CommonizerTarget, builder: InlineSourceBuilder.ModuleBuilder.() -> Unit) {
            val dependenciesList = dependencies.getOrPut(target) { mutableListOf() }
            val dependency = inlineSourceBuilderFactory[target].createModule {
                builder()
                name = "${target.prettyName}-dependency-${dependenciesList.size}-$name"
            }
            dependenciesList.add(dependency)
        }

        @InlineSourcesCommonizationTestDsl
        fun registerDependency(target: String, builder: InlineSourceBuilder.ModuleBuilder.() -> Unit) {
            registerDependency(parseCommonizerTarget(target), builder)
        }

        @InlineSourcesCommonizationTestDsl
        fun simpleSingleSourceTarget(target: CommonizerTarget, @Language("kotlin") sourceContent: String) {
            target(target) {
                module {
                    source(sourceContent)
                }
            }
        }

        @InlineSourcesCommonizationTestDsl
        fun simpleSingleSourceTarget(target: String, @Language("kotlin") sourceCode: String) {
            simpleSingleSourceTarget(parseCommonizerTarget(target), sourceCode)
        }

        fun build(): Parameters = Parameters(
            outputTarget = outputTarget ?: SharedCommonizerTarget(targets.map { it.target }.toSet()),
            dependencies = dependencies.toTargetDependent(),
            targets = targets.toList()
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
                            .filterKeys { dependencyTarget -> dependencyTarget.isEqualOrAncestorOf(target) }.values.flatten()
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

        @InlineSourcesCommonizationTestDsl
        fun module(builder: InlineSourceBuilder.ModuleBuilder.() -> Unit) {
            modules = modules + inlineSourceBuilder.createModule(builder)
        }

        fun build(): Target = Target(target, modules = modules)
    }

    fun commonize(builder: ParametersBuilder.() -> Unit): HierarchicalCommonizationResult {
        val consumer = MockResultsConsumer()
        val testParameters = ParametersBuilder(this).also(builder).build()
        val commonizerParameters = testParameters.toCommonizerParameters(consumer)
        runCommonization(commonizerParameters)
        assertEquals(ResultsConsumer.Status.DONE, consumer.status)
        return HierarchicalCommonizationResult(
            inlineSourceTestFactory = DependencyAwareInlineSourceTestFactory(inlineSourceBuilder, testParameters.dependencies),
            testParameters = testParameters,
            commonizerParameters = commonizerParameters,
            results = consumer.modulesByTargets.mapValues { (_, collection) -> collection.toList() }
        )
    }


    private fun Parameters.toCommonizerParameters(
        resultsConsumer: ResultsConsumer,
        manifestDataProvider: NativeManifestDataProvider = MockNativeManifestDataProvider()
    ): CommonizerParameters {
        return CommonizerParameters(
            outputTarget = outputTarget,
            manifestProvider = TargetDependent(outputTarget.withAllAncestors()) { manifestDataProvider },
            dependenciesProvider = TargetDependent(outputTarget.withAllAncestors()) { target ->
                val explicitDependencies = dependencies.getOrNull(target).orEmpty().map { module -> createModuleDescriptor(module) }
                val implicitDependencies = listOfNotNull(if (target == outputTarget) DefaultBuiltIns.Instance.builtInsModule else null)
                val dependencies = explicitDependencies + implicitDependencies
                if (dependencies.isEmpty()) null
                else MockModulesProvider.create(dependencies)
            },
            targetProviders = TargetDependent(targets.map { it.target }) { commonizerTarget ->
                val target = targets.single { it.target == commonizerTarget }
                TargetProvider(
                    target = commonizerTarget,
                    modulesProvider = MockModulesProvider.create(target.modules.map { createModuleDescriptor(it) })
                )
            },
            resultsConsumer = resultsConsumer
        )
    }
}


/* ASSERTIONS */

fun HierarchicalCommonizationResult.getTarget(target: CommonizerTarget): List<ResultsConsumer.ModuleResult> {
    return this.results[target] ?: kotlin.test.fail("Missing target $target in results ${this.results.keys}")
}

fun HierarchicalCommonizationResult.getTarget(target: String): List<ResultsConsumer.ModuleResult> {
    return getTarget(parseCommonizerTarget(target))
}

fun HierarchicalCommonizationResult.assertCommonized(
    target: CommonizerTarget,
    moduleBuilder: InlineSourceBuilder.ModuleBuilder.() -> Unit
) {
    val inlineSourceTest = inlineSourceTestFactory[target]
    val commonizedModule = getTarget(target).assertSingleCommonizedModule()
    val referenceModule = inlineSourceTest.createModule {
        moduleBuilder()
    }
    assertModulesAreEqual(
        inlineSourceTest.createMetadata(referenceModule), commonizedModule.metadata, target
    )
}

fun HierarchicalCommonizationResult.assertCommonized(target: CommonizerTarget, @Language("kotlin") sourceContent: String) {
    assertCommonized(target) {
        source(sourceContent)
    }
}

fun HierarchicalCommonizationResult.assertCommonized(target: String, @Language("kotlin") sourceContent: String) =
    assertCommonized(parseCommonizerTarget(target), sourceContent)

fun HierarchicalCommonizationResult.assertCommonized(target: String, moduleBuilder: InlineSourceBuilder.ModuleBuilder.() -> Unit) =
    assertCommonized(parseCommonizerTarget(target), moduleBuilder)

fun Collection<ResultsConsumer.ModuleResult>.assertSingleCommonizedModule(): Commonized {
    kotlin.test.assertEquals(1, size, "Expected exactly one module. Found: ${this.map { it.libraryName }}")
    return assertIs(single(), "Expected single module to be 'Commonized'")
}
