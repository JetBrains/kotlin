/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import groovy.lang.Closure
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceSetVisibilityTest {
    @Test
    fun testBasicSuccessful(): Unit = with(SourceSetCompilationDsl()) {
        val commonMain = sourceSet("commonMain")
        val jvmMain = sourceSet("jvmMain", commonMain)

        val commonTest = sourceSet("commonTest") {
            requiresVisibilityOf(commonMain)
        }
        val jvmTest = sourceSet("jvmTest", commonTest) {
            requiresVisibilityOf(jvmMain)
        }

        val jvmMainCompilation = compilation("jvm/main", jvmMain)
        compilation("jvm/test", jvmTest, jvmMainCompilation)

        checkInferredSourceSetsVisibility(jvmTest.name, commonMain.name, jvmMain.name)

        // should not throw exception
        checkSourceSetVisibilityRequirements(sourceSets, compilationsBySourceSets)
    }

    @Test
    fun testFailureWithNoAssociation(): Unit = with(SourceSetCompilationDsl()) {
        val commonMain = sourceSet("commonMain")
        val jvmMain = sourceSet("jvmMain", commonMain)

        val commonTest = sourceSet("commonTest") {
            requiresVisibilityOf(commonMain)
        }
        val jvmTest = sourceSet("jvmTest", commonTest) {
            requiresVisibilityOf(jvmMain)
        }

        compilation("jvm/main", jvmMain)
        val jvmTestCompilation = compilation("jvm/test", jvmTest) // note: no association with jvmMainCompilation

        checkInferredSourceSetsVisibility("jvmTest", *arrayOf())

        assertFailsWith<UnsatisfiedSourceSetVisibilityException> {
            checkSourceSetVisibilityRequirements(setOf(jvmTest), compilationsBySourceSets)
        }.apply {
            assertEquals(jvmTest, sourceSet)
            assertEquals(emptyList(), visibleSourceSets)
            assertEquals(setOf(jvmMain), requiredButNotVisible)
            assertEquals(setOf(jvmTestCompilation), compilations)
        }

        assertFailsWith<UnsatisfiedSourceSetVisibilityException> {
            checkSourceSetVisibilityRequirements(setOf(commonTest), compilationsBySourceSets)
        }.apply {
            assertEquals(commonTest, sourceSet)
            assertEquals(emptyList(), visibleSourceSets)
            assertEquals(setOf(commonMain), requiredButNotVisible)
            assertEquals(setOf(jvmTestCompilation), compilations)
        }
    }

    private fun SourceSetCompilationDsl.checkInferredSourceSetsVisibility(
        forSourceSetName: String,
        vararg expectedVisibleSourceSetNames: String
    ) = assertEquals(
        setOf(*expectedVisibleSourceSetNames),
        getVisibleSourceSetsFromAssociateCompilations(
            compilationsBySourceSets.getValue(sourceSetsByName.getValue(forSourceSetName))
        ).mapTo(mutableSetOf<String>()) { it.name }
    )

    @Test
    fun testInferenceForHierarchy() = with(SourceSetCompilationDsl()) {
        for (suffix in listOf("Main", "Test")) {
            val common = sourceSet("common$suffix")
            val jvmAndJs = sourceSet("jvmAndJs$suffix", common) {
                sourceSetsByName["jvmAndJsMain"]?.let(this::requiresVisibilityOf)
            }
            val linuxAndJs = sourceSet("linuxAndJs$suffix", common) {
                sourceSetsByName["linuxAndJsMain"]?.let(this::requiresVisibilityOf)
            }
            val jvm = sourceSet("jvm$suffix", common, jvmAndJs) {
                sourceSetsByName["jvmMain"]?.let(this::requiresVisibilityOf)
            }
            val linux = sourceSet("linux$suffix", common, linuxAndJs) {
                sourceSetsByName["linuxMain"]?.let(this::requiresVisibilityOf)
            }
            val js = sourceSet("js$suffix", common, jvmAndJs, linuxAndJs) {
                sourceSetsByName["jsMain"]?.let(this::requiresVisibilityOf)
            }

            compilation("jvm / $suffix", jvm, *listOfNotNull(compilationsByName["jvm / Main"]).toTypedArray())
            compilation("linux / $suffix", linux, *listOfNotNull(compilationsByName["linux / Main"]).toTypedArray())
            compilation("js / $suffix", js, *listOfNotNull(compilationsByName["js / Main"]).toTypedArray())
        }

        checkInferredSourceSetsVisibility("commonMain", *arrayOf())
        checkInferredSourceSetsVisibility("jvmMain", *arrayOf())
        checkInferredSourceSetsVisibility("jvmAndJsMain", *arrayOf())

        checkInferredSourceSetsVisibility("commonTest", "commonMain")
        checkInferredSourceSetsVisibility("jvmAndJsTest", "commonMain", "jvmAndJsMain")
        checkInferredSourceSetsVisibility("linuxAndJsTest", "commonMain", "linuxAndJsMain")
        checkInferredSourceSetsVisibility("jvmTest", "commonMain", "jvmAndJsMain", "jvmMain")

        checkSourceSetVisibilityRequirements(sourceSets, compilationsBySourceSets)
    }

    @Test
    fun testInferenceThroughIndirectAssociation(): Unit = with(SourceSetCompilationDsl()) {
        for ((previousSuffix, suffix) in listOf(null, "Main", "Test", "IntegrationTest").zipWithNext()) {
            val common = sourceSet("common$suffix") {
                if (previousSuffix != null) requiresVisibilityOf(sourceSetsByName.getValue("common$previousSuffix"))
            }
            val jvm = sourceSet("jvm$suffix", common) {
                if (previousSuffix != null) requiresVisibilityOf(sourceSetsByName.getValue("jvm$previousSuffix"))
            }
            val js = sourceSet("js$suffix", common) {
                if (previousSuffix != null) requiresVisibilityOf(sourceSetsByName.getValue("js$previousSuffix"))
            }

            compilation("jvm / $suffix", jvm, *listOfNotNull(compilationsByName["jvm / $previousSuffix"]).toTypedArray())
            compilation("js / $suffix", js, *listOfNotNull(compilationsByName["js / $previousSuffix"]).toTypedArray())
        }

        checkInferredSourceSetsVisibility("commonIntegrationTest", "commonMain", "commonTest")
        checkInferredSourceSetsVisibility("jvmIntegrationTest", "commonMain", "jvmMain", "commonTest", "jvmTest")

        checkSourceSetVisibilityRequirements(sourceSets, compilationsBySourceSets)

        // Now break visibility between *Test and *Main:

        compilationsByName.getValue("jvm / Test").associateWith.clear()
        compilationsByName.getValue("js / Test").associateWith.clear()
        val commonIntegrationTest = sourceSetsByName.getValue("commonIntegrationTest")
        commonIntegrationTest.requiresVisibilityOf(sourceSetsByName.getValue("commonMain"))

        assertFailsWith<UnsatisfiedSourceSetVisibilityException> {
            checkSourceSetVisibilityRequirements(setOf(commonIntegrationTest), compilationsBySourceSets)
        }.apply {
            assertEquals(commonIntegrationTest, this.sourceSet)
            assertEquals(setOf("commonTest"), visibleSourceSets.map { it.name }.toSet())
            assertEquals(setOf("commonMain"), requiredButNotVisible.map { it.name }.toSet())
            assertEquals(setOf("jvm / IntegrationTest", "js / IntegrationTest"), compilations.map { it.name }.toSet())
        }
    }

    @Test
    fun testFailureWithPlatformSpecificRequirement(): Unit = with(SourceSetCompilationDsl()) {
        val commonMain = sourceSet("commonMain")
        val jvmMain = sourceSet("jvmMain", commonMain)
        val jsMain = sourceSet("jvmMain", commonMain)

        val commonTest = sourceSet("commonTest") {
            requiresVisibilityOf(jvmMain) // <- invalid requirement!
        }
        val jvmTest = sourceSet("jvmTest", commonTest) {
            requiresVisibilityOf(jvmMain)
        }
        val jsTest = sourceSet("jsTest", commonTest) {
            requiresVisibilityOf(jsMain)
        }

        val jvmMainCompilation = compilation("jvm / main", jvmMain)
        val jsMainCompilation = compilation("js / main", jsMain)
        val jvmTestCompilation = compilation("jvm / test", jvmTest, jvmMainCompilation)
        val jsTestCompilation = compilation("js / test", jsTest, jsMainCompilation)

        assertFailsWith<UnsatisfiedSourceSetVisibilityException> {
            checkSourceSetVisibilityRequirements(setOf(commonTest), compilationsBySourceSets)
        }.apply {
            assertEquals(commonTest, this.sourceSet)
            assertEquals(listOf(commonMain), visibleSourceSets)
            assertEquals(setOf(jvmMain), requiredButNotVisible)
            assertEquals(setOf(jvmTestCompilation, jsTestCompilation), compilations)
        }
    }
}

class SourceSetCompilationDsl {
    val compilations: Set<MockKotlinCompilation> get() = compilationsByName.values.toSet()
    val sourceSets: Set<MockKotlinSourceSet> get() = sourceSetsByName.values.toSet()

    val sourceSetsByName = mutableMapOf<String, MockKotlinSourceSet>()
    val compilationsByName = mutableMapOf<String, MockKotlinCompilation>()

    val compilationsBySourceSets: Map<KotlinSourceSet, Set<KotlinCompilation<*>>>
        get() = mutableMapOf<KotlinSourceSet, MutableSet<KotlinCompilation<*>>>().apply {
            compilations.forEach { compilation ->
                compilation.allKotlinSourceSets.filterIsInstance<MockKotlinSourceSet>().forEach { sourceSet ->
                    getOrPut(sourceSet) { mutableSetOf() }.add(compilation)
                }
            }
        }

    fun sourceSet(
        name: String,
        vararg dependsOn: MockKotlinSourceSet,
        configure: MockKotlinSourceSet.() -> Unit = { }
    ) = MockKotlinSourceSet(name).apply {
        dependsOn.forEach { dependsOn(it) }
        configure()
        sourceSetsByName[name] = this
    }

    fun compilation(
        name: String,
        defaultSourceSet: KotlinSourceSet,
        vararg associateWith: MockKotlinCompilation,
        configure: MockKotlinCompilation.() -> Unit = { }
    ) = MockKotlinCompilation(name, defaultSourceSet).apply {
        associateWith.forEach { associateWith(it) }
        configure()
        compilationsByName[name] = this
    }
}

class MockKotlinSourceSet(private val name: String) : KotlinSourceSet {
    override fun getName(): String = name

    override val dependsOn: MutableSet<KotlinSourceSet> = mutableSetOf()

    override val requiresVisibilityOf: MutableSet<KotlinSourceSet> = mutableSetOf()

    override fun dependsOn(other: KotlinSourceSet) {
        dependsOn += other
    }

    override fun requiresVisibilityOf(other: KotlinSourceSet) {
        requiresVisibilityOf += other
    }

    //region Not implemented
    override val kotlin: SourceDirectorySet get() = throw UnsupportedOperationException()

    override fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet = throw UnsupportedOperationException()
    override val resources: SourceDirectorySet get() = throw UnsupportedOperationException()
    override val languageSettings: LanguageSettingsBuilder get() = throw UnsupportedOperationException()
    override fun languageSettings(configureClosure: Closure<Any?>): LanguageSettingsBuilder = languageSettings
    override fun languageSettings(configure: LanguageSettingsBuilder.() -> Unit): LanguageSettingsBuilder = languageSettings
    override val apiMetadataConfigurationName: String get() = throw UnsupportedOperationException()
    override val implementationMetadataConfigurationName: String get() = throw UnsupportedOperationException()
    override val compileOnlyMetadataConfigurationName: String get() = throw UnsupportedOperationException()
    override val runtimeOnlyMetadataConfigurationName: String get() = throw UnsupportedOperationException()
    override val customSourceFilesExtensions: Iterable<String> get() = throw UnsupportedOperationException()
    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = throw UnsupportedOperationException()
    override fun dependencies(configureClosure: Closure<Any?>) = throw UnsupportedOperationException()
    override val apiConfigurationName: String get() = throw UnsupportedOperationException()
    override val implementationConfigurationName: String get() = throw UnsupportedOperationException()
    override val compileOnlyConfigurationName: String get() = throw UnsupportedOperationException()
    override val runtimeOnlyConfigurationName: String get() = throw UnsupportedOperationException()
    //endregion
}

class MockKotlinCompilation(
    override val compilationName: String,
    override val defaultSourceSet: KotlinSourceSet
) : KotlinCompilation<KotlinCommonOptions> {
    override val kotlinSourceSets: Set<KotlinSourceSet> = setOf(defaultSourceSet)

    override val allKotlinSourceSets: Set<KotlinSourceSet>
        get() = mutableSetOf<KotlinSourceSet>().apply {
            fun visit(sourceSet: KotlinSourceSet) {
                if (add(sourceSet)) {
                    sourceSet.dependsOn.forEach(::visit)
                }
            }
            visit(defaultSourceSet)
        }

    override fun source(sourceSet: KotlinSourceSet) = defaultSourceSet.dependsOn(sourceSet)

    override val defaultSourceSetName: String
        get() = defaultSourceSet.name

    override fun associateWith(other: KotlinCompilation<*>) {
        associateWith += other
    }

    override val associateWith: MutableList<KotlinCompilation<*>> = mutableListOf()

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = defaultSourceSet.run(configure)

    //region Not implemented
    override val target: KotlinTarget get() = throw UnsupportedOperationException()
    override val compileKotlinTaskProvider: TaskProvider<out KotlinCompile<KotlinCommonOptions>> get() = throw UnsupportedOperationException()
    override fun getAttributes(): AttributeContainer = throw UnsupportedOperationException()
    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = throw UnsupportedOperationException()
    override fun dependencies(configureClosure: Closure<Any?>) = throw UnsupportedOperationException()
    override val apiConfigurationName: String get() = throw UnsupportedOperationException()
    override val implementationConfigurationName: String get() = throw UnsupportedOperationException()
    override val compileOnlyConfigurationName: String get() = throw UnsupportedOperationException()
    override val runtimeOnlyConfigurationName: String get() = throw UnsupportedOperationException()
    override fun defaultSourceSet(configure: Closure<*>) = throw UnsupportedOperationException()
    override val compileDependencyConfigurationName: String get() = throw UnsupportedOperationException()
    override var compileDependencyFiles: FileCollection
        get() = throw UnsupportedOperationException()
        set(_) = throw UnsupportedOperationException()
    override val output: KotlinCompilationOutput get() = throw UnsupportedOperationException()
    override val compileKotlinTaskName: String get() = throw UnsupportedOperationException()
    override val compileKotlinTask: KotlinCompile<KotlinCommonOptions> get() = throw UnsupportedOperationException()
    override val kotlinOptions: KotlinCommonOptions get() = throw UnsupportedOperationException()
    override fun kotlinOptions(configure: KotlinCommonOptions.() -> Unit) = throw UnsupportedOperationException()
    override fun kotlinOptions(configure: Closure<*>) = throw UnsupportedOperationException()
    override fun attributes(configure: AttributeContainer.() -> Unit) = throw UnsupportedOperationException()
    override fun attributes(configure: Closure<*>) = throw UnsupportedOperationException()
    override val compileAllTaskName: String get() = throw UnsupportedOperationException()
    override val moduleName: String get() = throw UnsupportedOperationException()
    //endregion

    override fun toString(): String = "compilation '${name}'"
}
